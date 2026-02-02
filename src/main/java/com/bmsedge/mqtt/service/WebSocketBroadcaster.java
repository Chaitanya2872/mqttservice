package com.bmsedge.mqtt.service;

import com.bmsedge.mqtt.event.MqttDataEvent;
import com.bmsedge.mqtt.model.MqttDataEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Broadcasts MQTT data to WebSocket clients in real-time
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleMqttDataEvent(MqttDataEvent event) {
        MqttDataEntity data = event.getData();

        log.info("📡 Broadcasting: device={}, counter={}, occupancy={}",
                data.getDeviceId(), data.getCounterName(), data.getOccupancy());

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", data.getId());
        payload.put("deviceId", data.getDeviceId());
        payload.put("counterName", data.getCounterName());
        payload.put("occupancy", data.getOccupancy());
        payload.put("inCount", data.getInCount());
        payload.put("waitTime", data.getWaitTime());
        payload.put("timestamp", data.getTimestamp().toString());

        // Broadcast to all subscribers
        messagingTemplate.convertAndSend("/topic/mqtt-data", payload);

        // Broadcast to device-specific channel
        messagingTemplate.convertAndSend("/topic/mqtt-data/device/" + data.getDeviceId(), payload);

        // Broadcast to counter-specific channel
        messagingTemplate.convertAndSend("/topic/mqtt-data/counter/" + data.getCounterName(), payload);
    }
}