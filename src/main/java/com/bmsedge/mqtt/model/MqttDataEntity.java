package com.bmsedge.mqtt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MQTT Data Entity
 * Stores raw MQTT message data
 */
@Entity
@Table(name = "mqtt_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "counter_name", nullable = false)
    private String counterName;

    @Column(name = "occupancy")
    private Integer occupancy;

    @Column(name = "in_count")
    private Integer inCount;

    @Column(name = "wait_time")
    private Double waitTime;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}