package com.rs.platform.moverprocess;

import java.util.Map;

public interface MoverProcessCommand {
    String getId();

    String getTrackingKey();

    Map<String, Object> getHeaders();

    <V> V getHeader(String name, Class<V> valueType);

    MoverProcessCommand addHeader(String name, Object value);
}
