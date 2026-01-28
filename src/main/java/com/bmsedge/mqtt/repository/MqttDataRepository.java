package com.bmsedge.mqtt.repository;

import com.bmsedge.mqtt.model.MqttDataEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for MQTT Data
 * Using Pageable for database-agnostic queries
 */
@Repository
public interface MqttDataRepository extends JpaRepository<MqttDataEntity, Long> {

    /**
     * Find latest data by device ID
     * Usage: findLatestByDeviceId(deviceId, PageRequest.of(0, 1))
     */
    @Query("SELECT m FROM MqttDataEntity m WHERE m.deviceId = :deviceId ORDER BY m.timestamp DESC")
    List<MqttDataEntity> findLatestByDeviceId(@Param("deviceId") String deviceId, Pageable pageable);

    /**
     * Find latest data by counter name
     * Usage: findLatestByCounterName(counterName, PageRequest.of(0, 1))
     */
    @Query("SELECT m FROM MqttDataEntity m WHERE m.counterName = :counterName ORDER BY m.timestamp DESC")
    List<MqttDataEntity> findLatestByCounterName(@Param("counterName") String counterName, Pageable pageable);

    /**
     * Find all data by device ID, ordered by timestamp descending
     */
    @Query("SELECT m FROM MqttDataEntity m WHERE m.deviceId = :deviceId ORDER BY m.timestamp DESC")
    List<MqttDataEntity> findAllByDeviceId(@Param("deviceId") String deviceId);

    /**
     * Find recent records
     * Usage: findRecentRecords(PageRequest.of(0, limit))
     */
    @Query("SELECT m FROM MqttDataEntity m ORDER BY m.timestamp DESC")
    List<MqttDataEntity> findRecentRecords(Pageable pageable);

    /**
     * Find data by timestamp range
     */
    @Query("SELECT m FROM MqttDataEntity m WHERE m.timestamp BETWEEN :startTime AND :endTime ORDER BY m.timestamp DESC")
    List<MqttDataEntity> findByTimestampRange(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * Find data by counter name and timestamp range
     */
    @Query("SELECT m FROM MqttDataEntity m WHERE m.counterName = :counterName AND m.timestamp BETWEEN :startTime AND :endTime ORDER BY m.timestamp DESC")
    List<MqttDataEntity> findByCounterAndTimestampRange(@Param("counterName") String counterName,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * Count records by device ID
     */
    long countByDeviceId(String deviceId);

    /**
     * Get all unique device IDs
     */
    @Query("SELECT DISTINCT m.deviceId FROM MqttDataEntity m")
    List<String> findAllDeviceIds();

    /**
     * Get all unique counter names
     */
    @Query("SELECT DISTINCT m.counterName FROM MqttDataEntity m")
    List<String> findAllCounterNames();
}