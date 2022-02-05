package org.maxron.platform.moverprocess.libtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.maxron.platform.moverprocess.EnableMoverProcess;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("The Mover Process application tests")
@EnableMoverProcess
class MoverProcessLibTests extends BaseLibTest {

    @DisplayName("Checking the loading of the moverProcess context")
    @Test
    void moverProcessContextLoads() {
        assertNotNull(moverProcess);
        assertTrue(moverProcess.isRunning());
        assertEquals(moverProcess.workersNumber(), moverProcessProperties.getThreads());
    }

}
