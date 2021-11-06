package com.rs.platform.moverprocess.mstest.config;

import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rs.platform.moverprocess.mstest.fixtures.TwCommandExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import zipkin2.Span;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentLinkedDeque;

@Configuration
public class TwConfiguration {

    @Bean
    Tracing tracing() {
        ConcurrentLinkedDeque<Span> spans = new ConcurrentLinkedDeque<>();
        return Tracing.newBuilder()
                .currentTraceContext(CurrentTraceContext.Default.create())
                .spanReporter(spans::add)
                .build();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(DataSource h2DataSourceModePostgreSql) {
        return new DataSourceTransactionManager(h2DataSourceModePostgreSql);
    }

    @Bean
    TwCommandExecutor twCommandExecutor() {
        return new TwCommandExecutor();
    }
}
