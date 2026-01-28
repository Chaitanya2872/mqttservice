package com.bmsedge.mqtt.service;

import com.bmsedge.mqtt.dto.MqttMessageDTO;
import com.bmsedge.mqtt.model.MqttDataEntity;
import com.bmsedge.mqtt.repository.MqttDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MQTT Data Service
 * Processes and saves MQTT messages
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttDataService {

    private final MqttDataRepository mqttDataRepository;
    private final ObjectMapper objectMapper;

    /**
     * Process and save MQTT message
     */
    @Transactional
    public void processMqttMessage(String jsonPayload) {
        try {
            log.info("📨 Processing MQTT message: {}", jsonPayload);

            // Parse JSON to DTO
            MqttMessageDTO messageDTO = objectMapper.readValue(jsonPayload, MqttMessageDTO.class);

            // Validate
            if (!isValid(messageDTO)) {
                log.warn("⚠️ Invalid MQTT message: {}", jsonPayload);
                return;
            }

            // Convert DTO to Entity and save
            MqttDataEntity entity = convertToEntity(messageDTO);
            MqttDataEntity savedEntity = mqttDataRepository.save(entity);

            log.info("✅ Saved MQTT data: id={}, device={}, counter={}, occupancy={}, inCount={}, waitTime={}",
                    savedEntity.getId(),
                    savedEntity.getDeviceId(),
                    savedEntity.getCounterName(),
                    savedEntity.getOccupancy(),
                    savedEntity.getInCount(),
                    savedEntity.getWaitTime());

        } catch (Exception e) {
            log.error("❌ Error processing MQTT message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process MQTT message", e);
        }
    }

    /**
     * Validate message
     */
    private boolean isValid(MqttMessageDTO dto) {
        if (dto.getDeviceId() == null || dto.getDeviceId().trim().isEmpty()) {
            log.warn("⚠️ Missing device_id");
            return false;
        }
        if (dto.getCounterName() == null || dto.getCounterName().trim().isEmpty()) {
            log.warn("⚠️ Missing counter_name");
            return false;
        }
        return true;
    }

    /**
     * Convert DTO to Entity - Save raw data as-is
     */
    private MqttDataEntity convertToEntity(MqttMessageDTO dto) {
        Double waitTime = dto.getWaitTimeInMinutes();

        return MqttDataEntity.builder()
                .deviceId(dto.getDeviceId())
                .counterName(dto.getCounterName())
                .occupancy(dto.getOccupancy() != null ? dto.getOccupancy() : 0)
                .inCount(dto.getInCount() != null ? dto.getInCount() : 0)
                .waitTime(waitTime)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Get latest data by device ID
     */
    public MqttDataEntity getLatestByDeviceId(String deviceId) {
        log.debug("Fetching latest data for device: {}", deviceId);
        try {
            Pageable pageable = PageRequest.of(0, 1);
            List<MqttDataEntity> results = mqttDataRepository.findLatestByDeviceId(deviceId, pageable);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("Error fetching latest data for device {}: {}", deviceId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch data for device: " + deviceId, e);
        }
    }

    /**
     * Get latest data by counter name
     */
    public MqttDataEntity getLatestByCounterName(String counterName) {
        log.debug("Fetching latest data for counter: {}", counterName);
        try {
            Pageable pageable = PageRequest.of(0, 1);
            List<MqttDataEntity> results = mqttDataRepository.findLatestByCounterName(counterName, pageable);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("Error fetching latest data for counter {}: {}", counterName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch data for counter: " + counterName, e);
        }
    }
}