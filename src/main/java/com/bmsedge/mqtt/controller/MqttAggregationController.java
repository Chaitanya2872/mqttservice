package com.bmsedge.mqtt.controller;

import com.bmsedge.mqtt.dto.MqttAggregationDTO;
import com.bmsedge.mqtt.service.MqttAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/mqtt-data/aggregate")
@RequiredArgsConstructor
public class MqttAggregationController {

    private final MqttAggregationService aggregationService;

    @GetMapping("/hourly")
    public List<MqttAggregationDTO> aggregateHourly(

            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {

        // Build day window safely
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(23, 59, 59);

        return aggregationService.aggregateHourly(from, to);
    }
}
