package com.bmsedge.mqtt.controller;

import com.bmsedge.mqtt.dto.MqttMessageDTO;
import com.bmsedge.mqtt.model.MqttDataEntity;
import com.bmsedge.mqtt.repository.MqttDataRepository;
import com.bmsedge.mqtt.service.MqttDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mqtt-data")
@RequiredArgsConstructor
public class MqttDataController {

    private final MqttDataService mqttDataService;
    private final MqttDataRepository mqttDataRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitMessage(@RequestBody MqttMessageDTO message) {
        try {
            log.info("📨 Manual message submission: {}", message);
            String jsonPayload = objectMapper.writeValueAsString(message);
            log.debug("Converted JSON payload: {}", jsonPayload);
            mqttDataService.processMqttMessage(jsonPayload);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Data saved successfully");
            response.put("timestamp", LocalDateTime.now());
            response.put("deviceId", message.getDeviceId());
            response.put("counterName", message.getCounterName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error submitting message: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            response.put("details", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/device/{deviceId}/latest")
    public ResponseEntity<?> getLatestByDevice(
            @PathVariable("deviceId") String deviceId
    ) {
        try {
            log.info("🔍 Fetching latest data for device: {}", deviceId);

            MqttDataEntity data =
                    mqttDataService.getLatestByDeviceId(deviceId);

            if (data == null) {
                log.warn("⚠️ No data found for device: {}", deviceId);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "not_found");
                response.put("message", "No data found for device: " + deviceId);
                response.put("deviceId", deviceId);
                response.put("timestamp", LocalDateTime.now());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            log.info("✅ Found data for device {}: id={}, counter={}",
                    deviceId, data.getId(), data.getCounterName());

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("❌ Error fetching latest data for device {}", deviceId, e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error fetching data");
            response.put("details", e.getClass().getSimpleName());
            response.put("deviceId", deviceId);

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/counter/{counterName}/latest")
    public ResponseEntity<?> getLatestByCounter(
            @PathVariable("counterName") String counterName
    ) {
        try {
            log.info("🔍 Fetching latest data for counter: {}", counterName);

            MqttDataEntity data =
                    mqttDataService.getLatestByCounterName(counterName);

            if (data == null) {
                log.warn("⚠️ No data found for counter: {}", counterName);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "not_found");
                response.put("message", "No data found for counter: " + counterName);
                response.put("counterName", counterName);
                response.put("timestamp", LocalDateTime.now());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            log.info("✅ Found data for counter {}: id={}, device={}",
                    counterName, data.getId(), data.getDeviceId());

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            log.error("❌ Error fetching latest data for counter {}", counterName, e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error fetching data");
            response.put("details", e.getClass().getSimpleName());
            response.put("counterName", counterName);

            return ResponseEntity.internalServerError().body(response);
        }
    }


    @GetMapping("/device/{deviceId}")
    public ResponseEntity<?> getAllByDevice(
            @PathVariable("deviceId") String deviceId
    ) {
        try {
            log.info("🔍 Fetching all data for device: {}", deviceId);

            List<MqttDataEntity> data =
                    mqttDataRepository.findAllByDeviceId(deviceId);

            log.info("✅ Found {} records for device: {}", data.size(), deviceId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("deviceId", deviceId);
            response.put("count", data.size());
            response.put("data", data);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching all data for device {}", deviceId, e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error fetching data");
            response.put("details", e.getClass().getSimpleName());
            response.put("deviceId", deviceId);

            return ResponseEntity.internalServerError().body(response);
        }
    }


    @GetMapping("/recent")
    public ResponseEntity<?> getRecent(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        try {
            log.info("🔍 Fetching recent data with limit: {}", limit);

            if (limit <= 0 || limit > 1000) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Limit must be between 1 and 1000");
                return ResponseEntity.badRequest().body(response);
            }

            Pageable pageable = PageRequest.of(0, limit);
            List<MqttDataEntity> recent = mqttDataRepository.findRecentRecords(pageable);

            log.info("✅ Found {} recent records", recent.size());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", recent.size());
            response.put("limit", limit);
            response.put("data", recent);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching recent data", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error fetching recent data");
            response.put("details", e.getClass().getSimpleName());

            return ResponseEntity.internalServerError().body(response);
        }
    }


    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            long totalRecords = mqttDataRepository.count();
            stats.put("status", "success");
            stats.put("totalRecords", totalRecords);
            stats.put("timestamp", LocalDateTime.now());
            if (totalRecords > 0) {
                Pageable pageable = PageRequest.of(0, 1);
                List<MqttDataEntity> recent = mqttDataRepository.findRecentRecords(pageable);
                if (!recent.isEmpty()) {
                    MqttDataEntity lastRecord = recent.get(0);
                    stats.put("lastMessageTime", lastRecord.getTimestamp());
                    stats.put("lastDeviceId", lastRecord.getDeviceId());
                    stats.put("lastCounterName", lastRecord.getCounterName());
                    stats.put("lastOccupancy", lastRecord.getOccupancy());
                    stats.put("lastInCount", lastRecord.getInCount());
                    stats.put("lastWaitTime", lastRecord.getWaitTime());
                }
                try {
                    List<String> deviceIds = mqttDataRepository.findAllDeviceIds();
                    List<String> counterNames = mqttDataRepository.findAllCounterNames();
                    stats.put("uniqueDevices", deviceIds.size());
                    stats.put("uniqueCounters", counterNames.size());
                    stats.put("deviceIds", deviceIds);
                    stats.put("counterNames", counterNames);
                } catch (Exception e) {
                    log.warn("Could not fetch unique devices/counters: {}", e.getMessage());
                }
            }
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Error fetching stats: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error fetching stats: " + e.getMessage());
            response.put("details", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAll() {
        try {
            long count = mqttDataRepository.count();
            mqttDataRepository.deleteAll();
            log.info("🗑️ Deleted {} records", count);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All data deleted");
            response.put("deletedCount", count);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error deleting all data: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error deleting data: " + e.getMessage());
            response.put("details", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "MQTT Data Service");
        health.put("timestamp", LocalDateTime.now());
        try {
            long count = mqttDataRepository.count();
            health.put("databaseConnected", true);
            health.put("recordCount", count);
            if (count > 0) {
                Pageable pageable = PageRequest.of(0, 1);
                List<MqttDataEntity> testRecent = mqttDataRepository.findRecentRecords(pageable);
                health.put("queryMethodsWorking", !testRecent.isEmpty());
                if (!testRecent.isEmpty()) {
                    MqttDataEntity latest = testRecent.get(0);
                    health.put("latestDeviceId", latest.getDeviceId());
                    health.put("latestCounterName", latest.getCounterName());
                }
            }
        } catch (Exception e) {
            health.put("databaseConnected", false);
            health.put("error", e.getMessage());
            health.put("errorType", e.getClass().getSimpleName());
            log.error("❌ Health check failed: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(health);
    }
}