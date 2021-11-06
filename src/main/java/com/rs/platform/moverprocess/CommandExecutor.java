package com.rs.platform.moverprocess;

@FunctionalInterface
public interface CommandExecutor<T extends MoverProcessCommand> {
    void executeInMoverTransaction(CommandExecution executionContext, T command);
}
