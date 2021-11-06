package com.rs.platform.moverprocess.mstest.fixtures;

import com.rs.platform.moverprocess.AbstractMoverProcessCommand;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
public class TwMoverProcessCommand extends AbstractMoverProcessCommand {
    private final TwDataEvent dataEvent;

    public TwMoverProcessCommand(TwDataEvent dataEvent, String trackingKey) {
        super(UUID.randomUUID().toString(), trackingKey);
        this.dataEvent = dataEvent;
    }

}
