package org.maxron.platform.moverprocess;

@FunctionalInterface
public interface CommandExecutor<T extends Command> {
    void executeInMoverTransaction(CommandExecution executionContext, T command);
}
