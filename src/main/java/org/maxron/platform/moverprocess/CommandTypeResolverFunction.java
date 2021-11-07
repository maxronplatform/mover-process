package org.maxron.platform.moverprocess;


import java.util.function.Function;

@SuppressWarnings("unchecked")
class CommandTypeResolverFunction implements Function<String, Class<? extends Command>> {
    @Override
    public Class<? extends Command> apply(String classname) {
        try {
            return (Class<? extends Command>) getClass().getClassLoader().loadClass(classname);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
