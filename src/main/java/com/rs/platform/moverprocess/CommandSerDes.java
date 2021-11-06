package com.rs.platform.moverprocess;

public interface CommandSerDes {
    <T extends Command> String serialize(T command);

    <T extends Command> T deserialize(String dataJson, Class<T> targetClass);
}
