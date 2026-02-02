package com.bmsedge.mqtt.event;

import com.bmsedge.mqtt.model.MqttDataEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when new MQTT data is saved
 * Triggers real-time WebSocket broadcasts
 */
@Getter
public class MqttDataEvent extends ApplicationEvent {

    private final MqttDataEntity data;

    public MqttDataEvent(Object source, MqttDataEntity data) {
        super(source);
        this.data = data;
    }
}