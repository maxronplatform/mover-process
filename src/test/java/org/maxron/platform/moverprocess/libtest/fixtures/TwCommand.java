package org.maxron.platform.moverprocess.libtest.fixtures;

import org.maxron.platform.moverprocess.AbstractCommand;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
public class TwCommand extends AbstractCommand {
    private final TwDataEvent dataEvent;

    public TwCommand(TwDataEvent dataEvent, String trackingKey) {
        super(UUID.randomUUID().toString(), trackingKey);
        this.dataEvent = dataEvent;
    }

}
