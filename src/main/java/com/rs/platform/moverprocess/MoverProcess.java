package com.rs.platform.moverprocess;

public interface MoverProcess extends AutoCloseable {
    void start();

    void stop();

    @Override
    default void close() {
        stop();
    }

    int aliveWorkers();

    int workersNumber();

    boolean isRunning();

    void submit(MoverProcessCommand moverProcessCommand);
}
