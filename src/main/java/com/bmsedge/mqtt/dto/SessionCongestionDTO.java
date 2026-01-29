package com.bmsedge.mqtt.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SessionCongestionDTO {

    private String counterName;

    // Session metrics
    private Double weightedCongestionIndex; // %
    private Long sessionMinutes;

    // Time-aware congestion breakdown
    private List<CongestionBlockDTO> blocks;
}
