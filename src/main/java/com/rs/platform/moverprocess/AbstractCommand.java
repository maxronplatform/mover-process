package com.rs.platform.moverprocess;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractCommand implements Command {
    private String id;
    private String trackingKey;
    private Map<String, Object> headers;

    protected AbstractCommand() {
    }

    public AbstractCommand(@JsonProperty("id") String id,
                           @JsonProperty("trackingKey") String trackingKey) {
        this.id = id;
        this.trackingKey = trackingKey;
        this.headers = new HashMap<>();
    }

    public AbstractCommand(@JsonProperty("id") String id,
                           @JsonProperty("trackingKey") String trackingKey,
                           @JsonProperty("headers") Map<String, Object> headers) {
        this.id = id;
        this.trackingKey = trackingKey;
        this.headers = headers;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTrackingKey() {
        return trackingKey;
    }

    @Override
    public Map<String, Object> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getHeader(String name, Class<V> valueType) {
        return (V) headers.get(name);
    }

    @Override
    public Command addHeader(String name, Object value) {
        return addGenericHeader(name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractCommand that = (AbstractCommand) o;
        return Objects.equals(getTrackingKey(), that.getTrackingKey()) &&
                Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTrackingKey(), getId());
    }

    private Command addGenericHeader(@Nonnull String name, @Nonnull Object value) {
        Validate.notNull(name, "The validated header key is null");
        Validate.notNull(value, "The validated header value is null");
        headers.put(name, value);
        return this;
    }
}
