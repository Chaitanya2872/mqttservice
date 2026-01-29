package com.bmsedge.mqtt.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PeakCongestionDTO {

    private String level;
    private int weight;

    // Peak wait time DURING this congestion block
    private Double peakWaitTimeInBlock;

    private LocalDateTime start;
    private LocalDateTime end;
    private Long durationMinutes;
}
