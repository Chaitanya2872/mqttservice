package com.bmsedge.mqtt.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MqttAggregationDTO {

    private String counterName;
    private Long totalCount;
    private Long peakQueue;
    private Double peakWaitTime;
    private LocalDateTime periodStart;

    // ✅ NEW — congestion analytics
    private Double congestionIndex;
    private PeakCongestionDTO peakCongestion;

    public MqttAggregationDTO(
            String counterName,
            Long totalCount,
            Long peakQueue,
            Double peakWaitTime,
            LocalDateTime periodStart
    ) {
        this.counterName = counterName;
        this.totalCount = totalCount;
        this.peakQueue = peakQueue;
        this.peakWaitTime = peakWaitTime;
        this.periodStart = periodStart;
    }
}
