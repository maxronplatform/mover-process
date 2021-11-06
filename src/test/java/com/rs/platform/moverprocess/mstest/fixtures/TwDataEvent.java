package com.rs.platform.moverprocess.mstest.fixtures;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;


@ToString
@Getter
public class TwDataEvent {
    private TwData data;
    private String eventId;
    private String eventType;
    private LocalDateTime eventTimestamp;
}
