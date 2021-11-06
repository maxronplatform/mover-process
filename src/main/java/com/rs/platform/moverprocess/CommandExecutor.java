package com.rs.platform.moverprocess;

@FunctionalInterface
public interface CommandExecutor<T extends Command> {
    void executeInMoverTransaction(CommandExecution executionContext, T command);
}
