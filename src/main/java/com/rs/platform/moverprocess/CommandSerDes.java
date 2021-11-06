package com.rs.platform.moverprocess;

public interface CommandSerDes {
    <T extends MoverProcessCommand> String serialize(T command);

    <T extends MoverProcessCommand> T deserialize(String dataJson, Class<T> targetClass);
}
