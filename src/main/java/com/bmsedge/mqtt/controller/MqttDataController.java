package com.bmsedge.mqtt.controller;

import com.bmsedge.mqtt.model.MqttDataEntity;
import com.bmsedge.mqtt.repository.MqttDataRepository;
import com.bmsedge.mqtt.service.MqttDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MQTT Data REST Controller
 * Provides API endpoints for device service to fetch live and historical data
 */
@Slf4j
@RestController
@RequestMapping("/api/mqtt-data")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MqttDataController {

    private final MqttDataService mqttDataService;
    private final MqttDataRepository mqttDataRepository;

    /**
     * Get latest single record for a device
     * Used by LiveCounterStatusService.getCounterLiveStatus()
     *
     * Returns direct entity fields (no wrapper) on success
     * Returns {"status": "not_found"} on failure
     */
    @GetMapping("/device/{deviceId}/latest")
    public ResponseEntity<Map<String, Object>> getLatestByDevice(@PathVariable String deviceId) {
        log.debug("📊 Fetching latest data for device: {}", deviceId);

        MqttDataEntity data = mqttDataService.getLatestByDeviceId(deviceId);

        if (data == null) {
            // Return 404 with status field so LiveCounterStatusService knows it failed
            Map<String, Object> response = new HashMap<>();
            response.put("status", "not_found");
            response.put("message", "No data found for device: " + deviceId);
            return ResponseEntity.status(404).body(response);
        }

        // Return direct fields (no wrapper) - LiveCounterStatusService checks for absence of "status" field
        Map<String, Object> response = new HashMap<>();
        response.put("id", data.getId());
        response.put("deviceId", data.getDeviceId());
        response.put("counterName", data.getCounterName());
        response.put("occupancy", data.getOccupancy());
        response.put("inCount", data.getInCount());
        response.put("waitTime", data.getWaitTime());
        response.put("timestamp", data.getTimestamp());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecent(@RequestParam(defaultValue = "10") int limit) {
        return getLatest(limit);  // Alias for /latest
    }

    /**
     * Get all historical records for a device
     * Used by LiveCounterStatusService.getOccupancyTrends()
     *
     * Returns {"status": "success", "data": [...]} format
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<Map<String, Object>> getAllByDevice(
            @PathVariable String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.debug("📊 Fetching all data for device: {} (start: {}, end: {})", deviceId, startTime, endTime);

        List<MqttDataEntity> dataList;

        if (startTime != null && endTime != null) {
            // Query with time range - need to add this to repository
            dataList = mqttDataRepository.findByDeviceIdAndTimestampBetween(deviceId, startTime, endTime);
        } else {
            // Get all records for device
            dataList = mqttDataRepository.findAllByDeviceId(deviceId);
        }

        // Convert to Map format expected by LiveCounterStatusService
        List<Map<String, Object>> data = dataList.stream()
                .map(this::entityToMap)
                .collect(Collectors.toList());

        // LiveCounterStatusService expects this exact format
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        response.put("count", data.size());
        response.put("deviceId", deviceId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get latest records for multiple devices (useful for counter aggregation)
     */
    @GetMapping("/devices/latest")
    public ResponseEntity<Map<String, Object>> getLatestForDevices(@RequestParam List<String> deviceIds) {
        log.debug("📊 Fetching latest data for {} devices", deviceIds.size());

        Map<String, Map<String, Object>> deviceDataMap = new HashMap<>();

        for (String deviceId : deviceIds) {
            MqttDataEntity data = mqttDataService.getLatestByDeviceId(deviceId);
            if (data != null) {
                deviceDataMap.put(deviceId, entityToMap(data));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("devices", deviceDataMap);
        response.put("requestedCount", deviceIds.size());
        response.put("foundCount", deviceDataMap.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get latest data by counter name
     */
    @GetMapping("/counter/{counterName}/latest")
    public ResponseEntity<Map<String, Object>> getLatestByCounter(@PathVariable String counterName) {
        log.debug("📊 Fetching latest data for counter: {}", counterName);

        MqttDataEntity data = mqttDataService.getLatestByCounterName(counterName);

        if (data == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "not_found");
            response.put("message", "No data found for counter: " + counterName);
            return ResponseEntity.status(404).body(response);
        }

        return ResponseEntity.ok(entityToMap(data));
    }

    /**
     * Get all records for a counter within time range
     */
    @GetMapping("/counter/{counterName}")
    public ResponseEntity<Map<String, Object>> getAllByCounter(
            @PathVariable String counterName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.debug("📊 Fetching all data for counter: {} (start: {}, end: {})", counterName, startTime, endTime);

        List<MqttDataEntity> dataList;

        if (startTime != null && endTime != null) {
            dataList = mqttDataRepository.findByCounterAndTimestampRange(counterName, startTime, endTime);
        } else {
            dataList = mqttDataRepository.findLatestByCounterName(
                    counterName,
                    PageRequest.of(0, 1000) // Last 1000 records
            );
        }

        List<Map<String, Object>> data = dataList.stream()
                .map(this::entityToMap)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        response.put("count", data.size());
        response.put("counterName", counterName);

        return ResponseEntity.ok(response);
    }

    /**
     * Get latest records (for dashboard overview)
     */
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> getLatest(@RequestParam(defaultValue = "10") int limit) {
        log.debug("📊 Fetching latest {} records", limit);

        List<MqttDataEntity> dataList = mqttDataRepository.findRecentRecords(PageRequest.of(0, limit));

        List<Map<String, Object>> data = dataList.stream()
                .map(this::entityToMap)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        response.put("count", data.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all unique device IDs
     */
    @GetMapping("/devices")
    public ResponseEntity<Map<String, Object>> getAllDevices() {
        log.debug("📊 Fetching all device IDs");

        List<String> devices = mqttDataRepository.findAllDeviceIds();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("devices", devices);
        response.put("count", devices.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all unique counter names
     */
    @GetMapping("/counters")
    public ResponseEntity<Map<String, Object>> getAllCounters() {
        log.debug("📊 Fetching all counter names");

        List<String> counters = mqttDataRepository.findAllCounterNames();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("counters", counters);
        response.put("count", counters.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get system statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.debug("📊 Fetching system statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", mqttDataRepository.count());
        stats.put("deviceCount", mqttDataRepository.findAllDeviceIds().size());
        stats.put("counterCount", mqttDataRepository.findAllCounterNames().size());

        List<MqttDataEntity> latest = mqttDataRepository.findRecentRecords(PageRequest.of(0, 1));
        if (!latest.isEmpty()) {
            stats.put("lastUpdate", latest.get(0).getTimestamp());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("statistics", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "MQTT Data Service");
        health.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(health);
    }

    // ==================== HELPER METHOD ====================

    /**
     * Convert entity to Map format
     */
    private Map<String, Object> entityToMap(MqttDataEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("deviceId", entity.getDeviceId());
        map.put("counterName", entity.getCounterName());
        map.put("occupancy", entity.getOccupancy());
        map.put("inCount", entity.getInCount());
        map.put("waitTime", entity.getWaitTime());
        map.put("timestamp", entity.getTimestamp());
        map.put("createdAt", entity.getCreatedAt());
        return map;
    }
}