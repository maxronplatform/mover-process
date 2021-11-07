package org.maxron.platform.moverprocess.mstest;

import org.maxron.platform.moverprocess.EnableMoverProcess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("The Mover Process application tests")
@EnableMoverProcess
public class MoverProcessMsTests extends BaseMsTest {

    @DisplayName("Checking the loading of the application context")
    @Test
    void contextLoads() {
        assertNotNull(moverProcess);
    }

}
