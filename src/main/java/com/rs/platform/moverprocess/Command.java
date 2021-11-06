package com.rs.platform.moverprocess;

import java.util.Map;

public interface Command {
    String getId();

    String getTrackingKey();

    Map<String, Object> getHeaders();

    <V> V getHeader(String name, Class<V> valueType);

    Command addHeader(String name, Object value);
}
