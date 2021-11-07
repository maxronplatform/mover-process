package org.maxron.platform.moverprocess;

import brave.ScopedSpan;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;

@Slf4j
class MoverProcessImpl<T extends Command> implements MoverProcess {
    private static final String TRACE_ID = "X-B3-TraceId";
    private static final String MOVER_TASK_HEADER_ORIGINAL_REQUEST_TRACE_ID = "MoverProcess.Internal.RequestTraceId";
    private static final String MOVER_TASK_HEADER_ORIGINAL_REQUEST_TRACE_CONTEXT = "MoverProcess.Internal.RequestTraceContext";

    private final MoverProcessDao moverProcessDao;
    private final TransactionTemplate submitTaskTxTemplate;
    private final TransactionTemplate processTasksTxTemplate;

    private final Lock lock = new ReentrantLock();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Condition newCommandAvailable = lock.newCondition();
    private final List<CommandWorker> commandWorkers = new ArrayList<>();

    private final String commandWorkerName;
    private final int threadCount;
    private final int pollCommandsTimeout;
    private final int retryCommandsTimeout;
    private final Map<String, CommandExecutor<T>> commandExecutors;
    private final Tracing tracing;

    private ExecutorService executorsService;

    MoverProcessImpl(String commandWorkerName, @Nonnull MoverProcessDao moverProcessDao, Map<String, CommandExecutor<T>> commandExecutors,
                     @Nonnull PlatformTransactionManager platformTransactionManager, Tracing tracing,
                     int pollCommandsTimeout, int retryCommandsTimeout, int threadCount) {
        this.commandWorkerName = commandWorkerName;
        this.moverProcessDao = moverProcessDao;
        this.commandExecutors = commandExecutors;

        // The open valid transaction is required to register a task
        this.submitTaskTxTemplate = new TransactionTemplate(platformTransactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY));
        this.processTasksTxTemplate = new TransactionTemplate(platformTransactionManager);
        this.pollCommandsTimeout = pollCommandsTimeout;
        this.retryCommandsTimeout = retryCommandsTimeout;
        this.threadCount = threadCount;
        this.tracing = tracing;
    }

    @Override
    public void start() {
        Validate.validState(!running.get(), "The validated moverProcess is already working");
        Validate.notEmpty(commandExecutors, "The validated commandExecutors is empty");
        running.set(true);
        this.executorsService = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            CommandWorker commandWorker = new CommandWorker(format("%s#%s", commandWorkerName, (i + 1)));
            this.executorsService.submit(commandWorker);
            commandWorkers.add(commandWorker);
        }
    }

    @Override
    public void stop() {
        stop(true);
    }

    @Override
    public int aliveWorkers() {
        return (int) commandWorkers.stream().filter(CommandWorker::isRunning).count();
    }

    @Override
    public int workersNumber() {
        return threadCount;
    }

    @Override
    public void submit(Command command) {
        Validate.notNull(command, "The validated command is null");
        propagateDiagnosticContext(command);
        submitTaskTxTemplate.execute((status) -> {
            try {
                moverProcessDao.createOrUpdateTrackingKey(command.getTrackingKey());
                moverProcessDao.addCommand(command);
            } catch (Exception e) {
                log.error(e.getMessage());
                status.setRollbackOnly();
            }
            return null;
        });
        signalTaskSubmitted();
    }

    private void propagateDiagnosticContext(Command command) {
        if (tracing != null) {
            propagateTracingContext(command);
        } else {
            propagateFallbackTraceId(command);
        }
    }

    private void propagateTracingContext(Command command) {
        TraceContext traceContext = tracing.currentTraceContext().get();
        if (traceContext != null) {
            MoverProcessTraceContextHolder traceContextHolder = MoverProcessTraceContextHolder.hold(traceContext);
            command.addHeader(MOVER_TASK_HEADER_ORIGINAL_REQUEST_TRACE_CONTEXT, traceContextHolder);
        }
    }

    private void propagateFallbackTraceId(Command command) {
        String traceId = MDC.get(TRACE_ID);
        if (traceId != null) {
            command.addHeader(MOVER_TASK_HEADER_ORIGINAL_REQUEST_TRACE_ID, traceId);
        }
    }

    private void signalTaskSubmitted() {
        lock.lock();
        try {
            newCommandAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void stop(boolean checkRunningState) {
        if (checkRunningState) {
            Validate.validState(running.get(), "Mover Process was not started");
        }
        executorsService.shutdown();
        running.set(false);
        signalTaskSubmitted();
        commandWorkers.clear();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private boolean shouldRun() {
        return running.get();
    }

    private class CommandWorker implements Runnable {
        private final Logger log = LoggerFactory.getLogger(getClass());
        private volatile boolean running = false;
        private final String name;
        private Tracer tracer;

        CommandWorker(String name) {
            this.name = name;
            if (tracing != null) {
                this.tracer = tracing.tracer();
            }
        }

        boolean isRunning() {
            return running;
        }

        @Override
        public void run() {
            running = true;
            log.info("Mover Process worker: '{}' - started", name);
            try {
                while (shouldRun()) {
                    CommandExecution commandExecution = processTasksTxTemplate.execute(status -> processNextMoverTaskInTransaction());
                    if (commandExecution != null && commandExecution.isSuspendRequested()) {
                        suspend();
                    }
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                stop(false);
            }
            log.info("Mover Process worker: '{}' - stopped", name);
            running = false;
        }

        private CommandExecution processNextMoverTaskInTransaction() {
            CommandExecution execution = new CommandExecution();

            String moverTaskKey = moverProcessDao.lockTrackingKey();
            if (moverTaskKey != null) {
                T command = moverProcessDao.latestCommandByTrackingKey(moverTaskKey);
                if (command != null) {
                    String commandId = command.getId();
                    log.debug("Mover Process worker: '{}' - task processing id: '{}', key: '{}'", name, commandId, moverTaskKey);
                    CommandExecutor<T> executor = commandExecutors.get(command.getClass().getCanonicalName());
                    runWithDiagnosticContextPropagation(command, () -> {
                        executor.executeInMoverTransaction(execution, command);
                        handleExecutionResults(execution, command);
                    });
                } else {
                    log.debug("Mover Process worker: '{}' - no commands for the key: '{}'", name, moverTaskKey);
                    moverProcessDao.removeByTrackingKey(moverTaskKey);
                    execution.ok();
                }
                moverProcessDao.updateTrackingKey(moverTaskKey);
            } else {
                log.trace("Mover Process worker: '{}' - pause before the next attempt to process task", name);
                execution.requestSuspend();
            }

            return execution;
        }

        private void handleExecutionResults(CommandExecution execution, Command command) {
            if (execution.isOk()) {
                log.debug("Mover Process worker: '{}', task id: '{}' has been processed", name, command.getId());
                moverProcessDao.removeCommand(command.getId());
            } else if (execution.hasToRetry()) {
                log.debug("Mover Process worker: '{}', task id: '{}' will be re-executed", name, command.getId());
                waitBeforeRetry();
            } else if (execution.hasToStop()) {
                log.debug("Mover Process worker: '{}', task id: '{}' has been processed", name, command.getId());
                Throwable error = execution.getError();
                if (error != null) {
                    log.error(format("Mover Process worker: '%s' will be stopped due to an error: %s", name, error.getMessage()), error);
                }
                stop(false);
            }
        }

        private void suspend() {
            lock.lock();
            try {
                newCommandAvailable.await(pollCommandsTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error(format("Mover Process worker error %s: %s", name, e.getMessage()), e);
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        private void waitBeforeRetry() {
            try {
                TimeUnit.MILLISECONDS.sleep(retryCommandsTimeout);
            } catch (InterruptedException e) {
                log.error(format("Mover Process worker error %s: %s", name, e.getMessage()), e);
                Thread.currentThread().interrupt();
            }
        }

        private void runWithDiagnosticContextPropagation(Command command, Runnable action) {
            if (tracer != null) {
                runWithTracingContextPropagation(command, action);
            } else {
                runWithFallbackTraceIdPropagation(command, action);
            }
        }

        private void runWithTracingContextPropagation(Command command, Runnable action) {
            String spanName = format("%s:%s", name, command.getClass().getCanonicalName());
            MoverProcessTraceContextHolder parentTraceContextHolder = command.getHeader(MOVER_TASK_HEADER_ORIGINAL_REQUEST_TRACE_CONTEXT, MoverProcessTraceContextHolder.class);
            TraceContext parentTraceContext = parentTraceContextHolder != null ? parentTraceContextHolder.toTraceContext() : null;
            ScopedSpan span = tracer.startScopedSpanWithParent(spanName, parentTraceContext);
            span.tag("commandId", command.getId());
            span.tag("trackingKey", command.getTrackingKey());
            try {
                action.run();
            } catch (Throwable e) {
                span.error(e);
                throw e;
            } finally {
                span.finish();
            }
        }

        private void runWithFallbackTraceIdPropagation(Command command, Runnable action) {
            String traceId = command.getHeader(MOVER_TASK_HEADER_ORIGINAL_REQUEST_TRACE_ID, String.class);
            if (traceId == null) {
                traceId = format("internal-%s", generateTraceId());
            }
            MDC.put(TRACE_ID, format("%s:%s", name, traceId));
            try {
                action.run();
            } finally {
                MDC.remove(TRACE_ID);
            }
        }

        private String generateTraceId() {
            UUID uuid = UUID.randomUUID();
            return Long.toHexString(uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits());
        }
    }
}
