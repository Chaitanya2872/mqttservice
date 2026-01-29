package com.bmsedge.mqtt.repository.view;

import java.time.LocalDateTime;

public interface CongestionTimelineView {
    String getCounterName();
    LocalDateTime getTimestamp();
    Double getWaitTime();
}
