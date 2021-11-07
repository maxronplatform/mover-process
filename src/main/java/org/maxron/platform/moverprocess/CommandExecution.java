package org.maxron.platform.moverprocess;

public class CommandExecution {
    private boolean ok = false;
    private boolean stop = false;
    private boolean retry = false;
    private boolean suspend = false;

    private Throwable error;

    public void ok() {
        ok = true;
    }

    public void retry() {
        retry = true;
    }

    public void stop(Throwable e) {
        stop = true;
        error = e;
    }

    void requestSuspend() {
        this.stop = false;
        this.ok = false;
        this.suspend = true;
    }

    boolean isOk() {
        return ok;
    }

    boolean hasToStop() {
        return stop;
    }

    Throwable getError() {
        return error;
    }

    boolean hasToRetry() {
        return retry;
    }

    boolean isSuspendRequested() {
        return suspend;
    }
}
