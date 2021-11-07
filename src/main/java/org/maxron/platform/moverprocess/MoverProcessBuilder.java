package org.maxron.platform.moverprocess;

import brave.Tracing;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class MoverProcessBuilder {
    private int threads = 3;
    private int pollCommandsTimeout = 2000;
    private int retryCommandsTimeout = 1000;

    private String commandWorkerName;
    private Tracing tracing;
    private MoverProcessDaoBuilder moverProcessDaoBuilder;
    private PlatformTransactionManager transactionManager;

    private final Map<String, CommandExecutor<Command>> commandExecutors = new HashMap<>();

    public MoverProcessBuilder() {
    }

    public MoverProcessBuilder commandWorkerName(@Nonnull String commandWorkerName) {
        this.commandWorkerName = commandWorkerName;
        return this;
    }

    public MoverProcessBuilder transactionManager(@Nonnull PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        return this;
    }

    public MoverProcessBuilder pollCommandsTimeout(int pollCommandsTimeout) {
        this.pollCommandsTimeout = pollCommandsTimeout;
        return this;
    }

    public MoverProcessBuilder retryCommandsTimeout(int retryCommandsTimeout) {
        this.retryCommandsTimeout = retryCommandsTimeout;
        return this;
    }

    public MoverProcessBuilder threads(int threads) {
        this.threads = threads;
        return this;
    }

    public MoverProcessBuilder moverDaoBuilder(@Nonnull MoverProcessDaoBuilder moverProcessDaoBuilder) {
        this.moverProcessDaoBuilder = moverProcessDaoBuilder;
        return this;
    }

    @SuppressWarnings("unchecked")
    public MoverProcessBuilder commandExecutors(@Nonnull List<CommandExecutor> commandExecutorsList) {
        commandExecutorsList.forEach((commandExecutor) -> {
            commandExecutors.put(canonicalCommandClassName(commandExecutor), commandExecutor);
        });
        return this;
    }

    public MoverProcessBuilder tracing(Tracing tracing) {
        this.tracing = tracing;
        return this;
    }

    public MoverProcess build() {
        Validate.notBlank(commandWorkerName, "The validated commandWorkerName is blank");
        Validate.notNull(transactionManager, "The validated transactionManager is null");
        Validate.notNull(moverProcessDaoBuilder, "The validated moverProcessDaoBuilder is null");
        Validate.validState(pollCommandsTimeout > 0, "The validated pollCommandsTimeout is less than or equal to 0");
        Validate.validState(threads > 0, "The validated threads is less than or equal to 0");
        Validate.validState(checkDataSourceConformance(moverProcessDaoBuilder, this.transactionManager), "Mismatch of DataSource and PlatformTransactionManager");

        MoverProcessDao moverProcessDao = moverProcessDaoBuilder.create();
        return new MoverProcessImpl<Command>(commandWorkerName, moverProcessDao, commandExecutors, transactionManager, tracing, pollCommandsTimeout, retryCommandsTimeout, threads);
    }

    private <T extends Command> String canonicalCommandClassName(CommandExecutor<T> commandExecutor) {
        return ((ParameterizedType) commandExecutor.getClass()
                .getGenericInterfaces()[0]).getActualTypeArguments()[0].getTypeName();
    }

    private boolean checkDataSourceConformance(MoverProcessDaoBuilder moverProcessDaoBuilder, PlatformTransactionManager transactionManager) {
        if (moverProcessDaoBuilder instanceof DbMoverProcessDaoBuilder) {
            if (transactionManager instanceof DataSourceTransactionManager) {
                DataSourceTransactionManager dsManager = (DataSourceTransactionManager) transactionManager;
                DataSource dataSource = ((DbMoverProcessDaoBuilder) moverProcessDaoBuilder).getDataSource();
                Validate.notNull(dataSource, "The validated dataSource is null");
                return dataSource.equals(dsManager.getDataSource());
            }
        }
        return true;
    }
}
