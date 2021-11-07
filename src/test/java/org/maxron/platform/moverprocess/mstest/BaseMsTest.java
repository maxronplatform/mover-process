package org.maxron.platform.moverprocess.mstest;

import brave.Tracing;
import org.maxron.platform.moverprocess.MoverProcess;
import org.maxron.platform.moverprocess.MoverProcessProperties;
import org.maxron.platform.moverprocess.mstest.config.TwConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, value = "classpath:dropDb.sql")
@EnableConfigurationProperties(MoverProcessProperties.class)
@SpringBootTest(
        classes = {
                TwConfiguration.class
        }
)
@ActiveProfiles("ms-test")
@Slf4j
class BaseMsTest {

    @Autowired
    protected MoverProcess moverProcess;

    @BeforeEach
    void setUp() {
        moverProcess.start();
    }

    @AfterEach
    void tearDown() {
        moverProcess.stop();
    }

    @AfterAll
    static void done() {
        Tracing current = Tracing.current();
        if (current != null) {
            current.close();
        }
    }
}
