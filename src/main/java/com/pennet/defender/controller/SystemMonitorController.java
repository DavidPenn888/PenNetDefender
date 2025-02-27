package com.pennet.defender.controller;

import com.pennet.defender.service.SystemMonitorService;
import com.pennet.defender.model.SystemStatus;
import com.pennet.defender.model.ThresholdAlert;
import com.pennet.defender.config.ThresholdConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class SystemMonitorController {

    @Autowired
    private SystemMonitorService systemMonitorService;

    @Autowired
    private ThresholdConfig thresholdConfig;

    @GetMapping("/status")
    public List<SystemStatus> getStatus(@RequestParam String timeline) {
        if ("day".equalsIgnoreCase(timeline)) {
            return systemMonitorService.getStatusLast24Hours();
        }
        return null; // Other time ranges can be added
    }

    @GetMapping("/get_threshold")
    public ThresholdConfig getThreshold() {
        return thresholdConfig;
    }

    @PostMapping("/change_threshold")
    public void changeThreshold(@RequestBody ThresholdConfig newThresholdConfig) {
        thresholdConfig.setCpuThreshold(newThresholdConfig.getCpuThreshold());
        thresholdConfig.setMemoryThreshold(newThresholdConfig.getMemoryThreshold());
        thresholdConfig.setStorageThreshold(newThresholdConfig.getStorageThreshold());
    }

    @GetMapping("/threshold_alert")
    public Map<String, Object> getThresholdAlerts(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "30") int size,
                                                  @RequestParam(required = false) String alertType) {
        Page<ThresholdAlert> thresholdAlerts = systemMonitorService.getThresholdAlerts(alertType, page, size);
        return Map.of(
                "totalPages", thresholdAlerts.getTotalPages(),
                "totalElements", thresholdAlerts.getTotalElements(),
                "content", thresholdAlerts.getContent()
        );
    }
}