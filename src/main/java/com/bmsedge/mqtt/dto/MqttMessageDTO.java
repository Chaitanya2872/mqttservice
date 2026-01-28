package com.bmsedge.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * MQTT Message DTO
 * Handles dynamic field names like Tandoor_occupancy, pan_pacific_occupancy, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttMessageDTO {

    @JsonProperty("device_id")
    private String deviceId;

    @JsonProperty("counter_name")
    private String counterName;

    // Store any additional properties dynamically
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        additionalProperties.put(name, value);
    }

    /**
     * Get occupancy from dynamic field name
     * Looks for: {counter_name}_occupancy or {counter_name}_oc_cupancy
     */
    public Integer getOccupancy() {
        // Try exact match first
        String occupancyKey = counterName + "_occupancy";
        if (additionalProperties.containsKey(occupancyKey)) {
            return convertToInteger(additionalProperties.get(occupancyKey));
        }

        // Try variations
        for (String key : additionalProperties.keySet()) {
            if (key.toLowerCase().contains("occupancy")) {
                return convertToInteger(additionalProperties.get(key));
            }
        }

        return 0;
    }

    /**
     * Get in_count from dynamic field name
     * Looks for: {counter_name}_incount
     */
    public Integer getInCount() {
        String inCountKey = counterName + "_incount";
        if (additionalProperties.containsKey(inCountKey)) {
            return convertToInteger(additionalProperties.get(inCountKey));
        }

        // Try variations
        for (String key : additionalProperties.keySet()) {
            if (key.toLowerCase().contains("incount") || key.toLowerCase().contains("in_count")) {
                return convertToInteger(additionalProperties.get(key));
            }
        }

        return 0;
    }

    /**
     * Get waiting time from dynamic field name
     * Looks for: {counter_name}_waiting_time_min
     */
    public String getWaitTime() {
        String waitTimeKey = counterName + "_waiting_time_min";
        if (additionalProperties.containsKey(waitTimeKey)) {
            Object value = additionalProperties.get(waitTimeKey);
            return value != null ? value.toString() : "0";
        }

        // Try variations
        for (String key : additionalProperties.keySet()) {
            if (key.toLowerCase().contains("waiting_time") || key.toLowerCase().contains("wait_time")) {
                Object value = additionalProperties.get(key);
                return value != null ? value.toString() : "0";
            }
        }

        return "0";
    }

    /**
     * Convert wait time string to minutes
     * "ready to serve" -> 0.0
     * "5 min" -> 5.0
     */
    public Double getWaitTimeInMinutes() {
        String waitTime = getWaitTime();

        if (waitTime == null || waitTime.trim().isEmpty()) {
            return 0.0;
        }

        String cleaned = waitTime.toLowerCase().trim();

        // Handle "ready to serve"
        if (cleaned.contains("ready")) {
            return 0.0;
        }

        try {
            // Extract numeric value
            String[] parts = cleaned.split("\\s+");
            for (String part : parts) {
                try {
                    return Double.parseDouble(part);
                } catch (NumberFormatException ignored) {
                    // Continue searching
                }
            }
        } catch (Exception e) {
            // Default to 0 if parsing fails
        }

        return 0.0;
    }

    /**
     * Helper to convert Object to Integer
     */
    private Integer convertToInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}