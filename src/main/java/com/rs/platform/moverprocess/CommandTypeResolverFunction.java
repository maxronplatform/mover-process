package com.rs.platform.moverprocess;


import java.util.function.Function;

@SuppressWarnings("unchecked")
class CommandTypeResolverFunction implements Function<String, Class<? extends MoverProcessCommand>> {
    @Override
    public Class<? extends MoverProcessCommand> apply(String classname) {
        try {
            return (Class<? extends MoverProcessCommand>) getClass().getClassLoader().loadClass(classname);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
