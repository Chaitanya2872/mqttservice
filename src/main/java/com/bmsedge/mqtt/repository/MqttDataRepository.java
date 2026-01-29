package com.bmsedge.mqtt.repository;

import com.bmsedge.mqtt.dto.MqttAggregationDTO;
import com.bmsedge.mqtt.model.MqttDataEntity;
import com.bmsedge.mqtt.repository.view.CongestionTimelineView;
import com.bmsedge.mqtt.repository.view.MqttAggregationView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MqttDataRepository extends JpaRepository<MqttDataEntity, Long> {

    /* ============================================================
       BASIC FETCH QUERIES (JPQL)
       ============================================================ */

    @Query("""
        SELECT m FROM MqttDataEntity m
        WHERE m.deviceId = :deviceId
        ORDER BY m.timestamp DESC
    """)
    List<MqttDataEntity> findLatestByDeviceId(
            @Param("deviceId") String deviceId,
            Pageable pageable
    );

    @Query("""
        SELECT m FROM MqttDataEntity m
        WHERE m.counterName = :counterName
        ORDER BY m.timestamp DESC
    """)
    List<MqttDataEntity> findLatestByCounterName(
            @Param("counterName") String counterName,
            Pageable pageable
    );

    @Query("""
        SELECT m FROM MqttDataEntity m
        WHERE m.deviceId = :deviceId
        ORDER BY m.timestamp DESC
    """)
    List<MqttDataEntity> findAllByDeviceId(@Param("deviceId") String deviceId);

    @Query("""
        SELECT m FROM MqttDataEntity m
        ORDER BY m.timestamp DESC
    """)
    List<MqttDataEntity> findRecentRecords(Pageable pageable);

    @Query("""
        SELECT m FROM MqttDataEntity m
        WHERE m.timestamp BETWEEN :startTime AND :endTime
        ORDER BY m.timestamp DESC
    """)
    List<MqttDataEntity> findByTimestampRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT m FROM MqttDataEntity m
        WHERE m.counterName = :counterName
          AND m.timestamp BETWEEN :startTime AND :endTime
        ORDER BY m.timestamp DESC
    """)
    List<MqttDataEntity> findByCounterAndTimestampRange(
            @Param("counterName") String counterName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    long countByDeviceId(String deviceId);

    @Query("SELECT DISTINCT m.deviceId FROM MqttDataEntity m")
    List<String> findAllDeviceIds();

    @Query("SELECT DISTINCT m.counterName FROM MqttDataEntity m")
    List<String> findAllCounterNames();


    /* ============================================================
       AGGREGATIONS (POSTGRESQL NATIVE)
       ============================================================ */

    /**
     * Per-minute aggregation for a counter
     * (PostgreSQL native because of date_trunc)
     */
    @Query(
            value = """
            SELECT
                date_trunc('minute', timestamp) AS periodStart,
                SUM(in_count) AS totalCount
            FROM mqtt_data
            WHERE counter_name = :counterName
              AND timestamp BETWEEN :from AND :to
            GROUP BY date_trunc('minute', timestamp)
            ORDER BY periodStart
        """,
            nativeQuery = true
    )
    List<MqttAggregationView> aggregatePerMinute(
            @Param("counterName") String counterName,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(
            value = """
        SELECT
            s.counter_name AS counterName,

            -- ✅ Footfall: max per session, summed
            SUM(s.session_max) AS totalCount,

            -- ✅ Peak queue (counter-wise, using in_count)
            MAX(s.peak_queue) AS peakQueue,

            -- ✅ Peak wait time (counter-wise)
            MAX(s.peak_wait_time) AS peakWaitTime,

            TO_CHAR(MIN(s.min_timestamp), 'YYYY-MM-DD HH24:MI:SS') AS periodStart
        FROM (
            SELECT
                counter_name,
                MAX(in_count) AS session_max,
                MAX(occupancy) AS peak_queue,
                MAX(wait_time) AS peak_wait_time,
                MIN(timestamp) AS min_timestamp
            FROM mqtt_data
            WHERE timestamp BETWEEN :from AND :to
              AND (
                    (CAST(timestamp AS time) >= TIME '06:55' AND CAST(timestamp AS time) < TIME '11:25')
                 OR (CAST(timestamp AS time) >= TIME '11:25' AND CAST(timestamp AS time) < TIME '15:25')
                 OR (CAST(timestamp AS time) >= TIME '15:35' AND CAST(timestamp AS time) <= TIME '19:00')
              )
            GROUP BY
                counter_name,
                CASE
                    WHEN CAST(timestamp AS time) >= TIME '06:55' AND CAST(timestamp AS time) < TIME '11:25' THEN 1
                    WHEN CAST(timestamp AS time) >= TIME '11:25' AND CAST(timestamp AS time) < TIME '15:25' THEN 2
                    WHEN CAST(timestamp AS time) >= TIME '15:35' AND CAST(timestamp AS time) <= TIME '19:00' THEN 3
                END
        ) s
        GROUP BY s.counter_name
    """,
            nativeQuery = true
    )
    List<MqttAggregationView> aggregateHourly(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(
            value = """
        SELECT
            counter_name AS counterName,
            timestamp     AS timestamp,
            wait_time     AS waitTime
        FROM mqtt_data
        WHERE timestamp BETWEEN :from AND :to
        ORDER BY counter_name, timestamp
    """,
            nativeQuery = true
    )
    List<CongestionTimelineView> fetchCongestionTimeline(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );





    @Query(
            value = """
        SELECT
            counter_name AS counterName,
            queue_length AS peakQueue,
            TO_CHAR(timestamp, 'YYYY-MM-DD HH24:MI:SS') AS peakQueueTime
        FROM mqtt_data
        WHERE timestamp BETWEEN :from AND :to
        ORDER BY queue_length DESC, timestamp
        LIMIT 1
    """,
            nativeQuery = true
    )
    List<MqttAggregationView> findPeakQueue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query(
            value = """
        SELECT
            counter_name AS counterName,
            wait_time AS peakWaitTime,
            TO_CHAR(timestamp, 'YYYY-MM-DD HH24:MI:SS') AS peakWaitTimeTime
        FROM mqtt_data
        WHERE timestamp BETWEEN :from AND :to
        ORDER BY wait_time DESC, timestamp
        LIMIT 1
    """,
            nativeQuery = true
    )
    List<MqttAggregationView> findPeakWaitTime(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


}