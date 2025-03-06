package com.pennet.defender.service;

import com.pennet.defender.model.SystemStatus;
import com.pennet.defender.model.ThresholdAlert;
import com.pennet.defender.repository.SystemStatusRepository;
import com.pennet.defender.repository.ThresholdAlertRepository;
import com.pennet.defender.config.ThresholdConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemMonitorService {

    @Autowired
    private SystemStatusRepository systemStatusRepository;

    @Autowired
    private ThresholdAlertRepository thresholdAlertRepository;

    @Autowired
    private ThresholdConfig thresholdConfig;

    public void saveSystemStatus(double cpuUsage, double memoryUsage, double storageUsage) {
        SystemStatus status = new SystemStatus();
        status.setTimestamp(LocalDateTime.now());
        status.setCpuUsage(cpuUsage);
        status.setMemoryUsage(memoryUsage);
        status.setStorageUsage(storageUsage);

        if (cpuUsage > thresholdConfig.getCpuThreshold()) {
            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "CPU", "CPU usage: " + cpuUsage + "%"));
        }
        if (memoryUsage > thresholdConfig.getMemoryThreshold()) {
            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "Memory", "Memory usage: " + memoryUsage + "%"));
        }
        if (storageUsage > thresholdConfig.getStorageThreshold()) {
            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "Storage", "Storage usage: " + storageUsage + "%"));
        }

        systemStatusRepository.save(status);
    }

//    public List<SystemStatus> getStatusLast24Hours() {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime dayAgo = now.minusHours(24);
//        return systemStatusRepository.findHighestUsagePerHourLast24Hours(dayAgo, now);
//    }
    public List<SystemStatus> getRecentStatus() {
        return systemStatusRepository.findTop20ByOrderByTimestampDesc();
    }

    public Page<ThresholdAlert> getThresholdAlerts(String alertType, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);  // Page index is 0-based
        if (alertType != null && !alertType.isEmpty()) {
            return thresholdAlertRepository.findByAlertTypeOrderByTimestampDesc(alertType, pageable);
        }
        return thresholdAlertRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
