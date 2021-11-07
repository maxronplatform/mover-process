package org.maxron.platform.moverprocess.mstest.fixtures;

import org.maxron.platform.moverprocess.CommandExecution;
import org.maxron.platform.moverprocess.CommandExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Component
@Slf4j
public class TwCommandExecutor implements CommandExecutor<TwCommand> {
    public static final String MDC_PREFIX = "tw-mdc-";

    @Override
    public void executeInMoverTransaction(CommandExecution executionContext, TwCommand command) {
        TwDataEvent event = command.getDataEvent();

        executeWithMdc(
                command.getHeaders(),
                () -> execute(executionContext, event, event.getEventId(), event.getEventType()));
    }

    private void executeWithMdc(Map<String, Object> headers, Runnable doIt) {
        if (headers != null) {
            Map<String, String> mdcHeaders = headers.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(MDC_PREFIX))
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

            for (Map.Entry<String, String> entry : mdcHeaders.entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
            }

            try {
                doIt.run();
            } finally {
                for (String entryKey : mdcHeaders.keySet()) {
                    MDC.remove(entryKey);
                }
            }
        } else {
            doIt.run();
        }
    }

    private void execute(CommandExecution commandExecution, Object event, String eventId, String eventType) {
        try {
            log.info("Sending the event: {}", event);

            TwMessageChannel messageChannel = new TwMessageChannel();

            String message = format("eventId - %s; eventType - %s", eventId, eventType);

            if (!messageChannel.send(message)) {
                log.info("Resending the event: {}", event);

                commandExecution.retry();
            } else {

                log.info("The event successfully sent: {}", event);
                commandExecution.ok();

            }

        } catch (Exception e) {
            if (isTransientException(e)) {
                commandExecution.retry();

            } else {

                log.warn("The event cannot be sent: {}", event);
                commandExecution.stop(e);
            }
        }
    }

    private boolean isTransientException(Exception e) {
        return ExceptionUtils.indexOfType(e, RuntimeException.class) > 0;
    }
}
