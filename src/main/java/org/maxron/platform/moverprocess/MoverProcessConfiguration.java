package org.maxron.platform.moverprocess;

import brave.Tracing;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

import static java.lang.String.format;


@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Configuration
@Slf4j
public class MoverProcessConfiguration implements ImportAware {

    protected AnnotationAttributes enableMover;

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    DataSource h2DataSourceModePostgreSql(MoverProcessProperties moverProcessProperties) {
        return new DriverManagerDataSource(format("%s;MODE=PostgreSQL", moverProcessProperties.getJdbcUrl()),
                moverProcessProperties.getUsername(), moverProcessProperties.getPassword());
    }

    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Bean
    public MoverProcess mover(Tracing tracing, ObjectMapper mapper, DataSource h2DataSourceModePostgreSql,
                              MoverProcessProperties moverProcessProperties, List<CommandExecutor> commandExecutors,
                              PlatformTransactionManager platformTransactionManager) {

        MoverProcessDaoBuilder commanderDaoBuilder = new DbMoverProcessDaoBuilder()
                .dataSource(h2DataSourceModePostgreSql)
                .commandSerDes(new JsonCommandSerDes(mapper))
                .tasksLogTableName(moverProcessProperties.getTasksLogTableName())
                .tracksTableName(moverProcessProperties.getTracksTableName());

        return new MoverProcessBuilder()
                .moverDaoBuilder(commanderDaoBuilder)
                .transactionManager(platformTransactionManager)
                .commandWorkerName(moverProcessProperties.getCommandWorkerName())
                .pollCommandsTimeout(moverProcessProperties.getPollCommandsTimeout())
                .retryCommandsTimeout(moverProcessProperties.getRetryCommandsTimeout())
                .threads(moverProcessProperties.getThreads())
                .commandExecutors(commandExecutors)
                .tracing(tracing)
                .build();
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableMover = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableMoverProcess.class.getName(), false));
        if (this.enableMover == null) {
            throw new IllegalArgumentException(
                    "@EnableMoverProcess is not present on importing class " + importMetadata.getClassName());
        }
    }
}
