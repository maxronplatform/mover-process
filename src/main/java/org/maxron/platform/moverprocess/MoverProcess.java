package org.maxron.platform.moverprocess;

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

    void submit(Command command);
}
