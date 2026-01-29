package com.bmsedge.mqtt.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CongestionBlockDTO {

    private String level;
    private int weight;

    private LocalDateTime start;
    private LocalDateTime end;

    private Long durationMinutes;
}
