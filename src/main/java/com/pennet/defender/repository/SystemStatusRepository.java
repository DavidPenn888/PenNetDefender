package com.pennet.defender.repository;

import com.pennet.defender.model.SystemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemStatusRepository extends JpaRepository<SystemStatus, Integer> {
    List<SystemStatus> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    // New query to get highest usage per hour for last 24 hours
    @Query(value =
            "SELECT ss.* FROM system_status ss " +
                    "INNER JOIN (" +
                    "    SELECT HOUR(timestamp) AS hour, " +
                    "           MAX(cpu_usage + memory_usage + storage_usage) AS max_total " +
                    "    FROM system_status " +
                    "    WHERE timestamp BETWEEN ?1 AND ?2 " +
                    "    GROUP BY HOUR(timestamp)" +
                    ") ranked ON HOUR(ss.timestamp) = ranked.hour " +
                    "AND (ss.cpu_usage + ss.memory_usage + ss.storage_usage) = ranked.max_total " +
                    "ORDER BY ss.timestamp DESC",
            nativeQuery = true)
    List<SystemStatus> findHighestUsagePerHourLast24Hours(LocalDateTime start, LocalDateTime end);
}