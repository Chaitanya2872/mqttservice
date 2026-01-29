package com.bmsedge.mqtt.service;

import com.bmsedge.mqtt.dto.CongestionBlockDTO;
import com.bmsedge.mqtt.dto.MqttAggregationDTO;
import com.bmsedge.mqtt.dto.PeakCongestionDTO;
import com.bmsedge.mqtt.dto.SessionCongestionDTO;
import com.bmsedge.mqtt.repository.MqttDataRepository;
import com.bmsedge.mqtt.repository.view.CongestionTimelineView;
import com.bmsedge.mqtt.repository.view.MqttAggregationView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MqttAggregationService {

    private final MqttDataRepository repository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<MqttAggregationDTO> aggregateHourly(
            LocalDateTime from,
            LocalDateTime to
    ) {

        List<MqttAggregationDTO> aggregates =
                repository.aggregateHourly(from, to)
                        .stream()
                        .map(view -> new MqttAggregationDTO(
                                view.getCounterName(),
                                view.getTotalCount(),
                                view.getPeakQueue(),
                                view.getPeakWaitTime(),
                                LocalDateTime.parse(
                                        view.getPeriodStart(), FORMATTER
                                )
                        ))
                        .collect(Collectors.toList());

        Map<String, List<CongestionTimelineView>> congestionMap =
                repository.fetchCongestionTimeline(from, to)
                        .stream()
                        .collect(Collectors.groupingBy(
                                CongestionTimelineView::getCounterName
                        ));

        for (MqttAggregationDTO dto : aggregates) {

            List<CongestionTimelineView> rows =
                    congestionMap.get(dto.getCounterName());

            if (rows == null || rows.isEmpty()) {
                dto.setPeakCongestion(null);
                continue;
            }

            dto.setPeakCongestion(
                    extractPeakCongestion(rows)
            );
        }

        return aggregates;
    }

    private PeakCongestionDTO extractPeakCongestion(
            List<CongestionTimelineView> rows
    ) {

        List<CongestionBlockDTO> blocks =
                buildCongestionBlocks(rows);

        return blocks.stream()
                .max(Comparator
                        .comparingInt(CongestionBlockDTO::getWeight)
                        .thenComparingLong(CongestionBlockDTO::getDurationMinutes)
                )
                .map(b -> {

                    PeakCongestionDTO dto = new PeakCongestionDTO();
                    dto.setLevel(b.getLevel());
                    dto.setWeight(b.getWeight());
                    dto.setStart(b.getStart());
                    dto.setEnd(b.getEnd());
                    dto.setDurationMinutes(b.getDurationMinutes());

                    double peakWaitInBlock = rows.stream()
                            .filter(r ->
                                    !r.getTimestamp().isBefore(b.getStart()) &&
                                            !r.getTimestamp().isAfter(b.getEnd())
                            )
                            .mapToDouble(CongestionTimelineView::getWaitTime)
                            .max()
                            .orElse(0);

                    dto.setPeakWaitTimeInBlock(peakWaitInBlock);

                    return dto;
                })
                .orElse(null);
    }



    private List<CongestionBlockDTO> buildCongestionBlocks(
            List<CongestionTimelineView> rows
    ) {

        List<CongestionBlockDTO> blocks = new ArrayList<>();
        CongestionBlockDTO current = null;

        for (CongestionTimelineView row : rows) {

            int weight = congestionWeight(row.getWaitTime());

            if (weight == 0) {
                if (current != null) {
                    finalizeBlock(current);
                    blocks.add(current);
                    current = null;
                }
                continue;
            }

            String level = congestionLevel(weight);

            if (current == null || current.getWeight() != weight) {

                if (current != null) {
                    finalizeBlock(current);
                    blocks.add(current);
                }

                current = new CongestionBlockDTO();
                current.setLevel(level);
                current.setWeight(weight);
                current.setStart(row.getTimestamp());
            }

            current.setEnd(row.getTimestamp());
        }

        if (current != null) {
            finalizeBlock(current);
            blocks.add(current);
        }

        return blocks;
    }




    public List<SessionCongestionDTO> computeSessionCongestion(
            LocalDateTime from,
            LocalDateTime to
    ) {

        long sessionMinutes =
                Duration.between(from, to).toMinutes();

        // 1️⃣ Fetch raw time-series data
        Map<String, List<CongestionTimelineView>> counterData =
                repository.fetchCongestionTimeline(from, to)
                        .stream()
                        .collect(Collectors.groupingBy(
                                CongestionTimelineView::getCounterName
                        ));

        List<SessionCongestionDTO> response = new ArrayList<>();

        // 2️⃣ Process each counter in one flow
        for (var entry : counterData.entrySet()) {

            String counterName = entry.getKey();
            List<CongestionTimelineView> rows = entry.getValue();

            List<CongestionBlockDTO> blocks = new ArrayList<>();

            CongestionBlockDTO current = null;

            // 3️⃣ Build sustained congestion blocks
            for (CongestionTimelineView row : rows) {

                int weight = congestionWeight(row.getWaitTime());
                String level = congestionLevel(weight);

                if (current == null || current.getWeight() != weight) {

                    if (current != null) {
                        finalizeBlock(current);
                        blocks.add(current);
                    }

                    current = new CongestionBlockDTO();
                    current.setLevel(level);
                    current.setWeight(weight);
                    current.setStart(row.getTimestamp());
                }

                current.setEnd(row.getTimestamp());
            }

            if (current != null) {
                finalizeBlock(current);
                blocks.add(current);
            }

            // 4️⃣ Compute Weighted Congestion Index
            long weightedSum = blocks.stream()
                    .mapToLong(b ->
                            b.getWeight() * b.getDurationMinutes()
                    )
                    .sum();

            double wci =
                    (weightedSum / (double) (sessionMinutes * 5)) * 100;

            // 5️⃣ Build response DTO
            SessionCongestionDTO dto = new SessionCongestionDTO();
            dto.setCounterName(counterName);
            dto.setSessionMinutes(sessionMinutes);
            dto.setBlocks(blocks);
            dto.setWeightedCongestionIndex(wci);

            response.add(dto);
        }

        return response;
    }

    /* ------------------ PRIVATE HELPERS ------------------ */

    private void finalizeBlock(CongestionBlockDTO block) {
        block.setDurationMinutes(
                Duration.between(
                        block.getStart(),
                        block.getEnd()
                ).toMinutes()
        );
    }

    private int congestionWeight(double waitTime) {
        if (waitTime <= 0) return 0;
        if (waitTime <= 2) return 0;
        if (waitTime == 3) return 0;
        if (waitTime == 4) return 1;
        if (waitTime <= 8) return 2;
        if (waitTime <= 12) return 3;
        return 5;
    }

    private String congestionLevel(int weight) {
        return switch (weight) {
            case 1 -> "High";
            case 2 -> "Critical";
            case 3 -> "Severe";
            case 5 -> "Extreme";
            default -> "Low";
        };
    }
}
