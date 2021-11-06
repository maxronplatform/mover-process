package com.rs.platform.moverprocess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.io.IOException;

class JsonCommandSerDes implements CommandSerDes {

    private final ObjectMapper objectMapper;

    JsonCommandSerDes(@Nonnull ObjectMapper objectMapper) {
        Validate.notNull(objectMapper, "The validated objectMapper is null");
        this.objectMapper = objectMapper.copy()
                .activateDefaultTyping(new LaissezFaireSubTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
    }

    @Override
    public <T extends MoverProcessCommand> String serialize(T command) {
        if (command == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends MoverProcessCommand> T deserialize(String dataJson, Class<T> targetClass) {
        if (dataJson == null || dataJson.isEmpty() || targetClass == null) {
            return null;
        }

        try {
            return objectMapper.readValue(dataJson, targetClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
