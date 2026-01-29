package com.bmsedge.mqtt.repository.view;

public interface MqttAggregationView {
    String getCounterName();
    Long getTotalCount();
    Long getPeakQueue();
    Double getPeakWaitTime();
    String getPeriodStart();  // STRING now
}