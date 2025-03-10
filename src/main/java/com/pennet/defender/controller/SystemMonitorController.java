package com.pennet.defender.controller;

import com.pennet.defender.model.ApiResponse;
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
@RequestMapping("/api/system")
public class SystemMonitorController {

    @Autowired
    private SystemMonitorService systemMonitorService;

    @Autowired
    private ThresholdConfig thresholdConfig;

//    @GetMapping("/status")
//    public List<SystemStatus> getStatus(@RequestParam String timeline) {
//        if ("recent".equalsIgnoreCase(timeline)) {
//            return systemMonitorService.getRecentStatus();
//        }
//        return null; // Other time ranges can be added
//    }
//
//    @GetMapping("/get_threshold")
//    public ThresholdConfig getThreshold() {
//        return thresholdConfig;
//    }
//
//    @PostMapping("/change_threshold")
//    public void changeThreshold(@RequestBody ThresholdConfig newThresholdConfig) {
//        thresholdConfig.setCpuThreshold(newThresholdConfig.getCpuThreshold());
//        thresholdConfig.setMemoryThreshold(newThresholdConfig.getMemoryThreshold());
//        thresholdConfig.setStorageThreshold(newThresholdConfig.getStorageThreshold());
//    }
//
//    @GetMapping("/threshold_alert")
//    public Map<String, Object> getThresholdAlerts(@RequestParam(defaultValue = "1") int page,
//                                                  @RequestParam(defaultValue = "30") int size,
//                                                  @RequestParam(required = false) String alertType) {
//        Page<ThresholdAlert> thresholdAlerts = systemMonitorService.getThresholdAlerts(alertType, page, size);
//        return Map.of(
//                "totalPages", thresholdAlerts.getTotalPages(),
//                "totalElements", thresholdAlerts.getTotalElements(),
//                "content", thresholdAlerts.getContent()
//        );
//    }
@GetMapping("/status")
public ApiResponse<List<SystemStatus>> getStatus(@RequestParam String timeline) {
    if ("recent".equalsIgnoreCase(timeline)) {
        List<SystemStatus> statuses = systemMonitorService.getRecentStatus();
        return new ApiResponse<>(0, statuses, "success");
    }
    return new ApiResponse<>(0, List.of(), "No data available");
}

    @GetMapping("/get_threshold")
    public ApiResponse<ThresholdConfig> getThreshold() {
        return new ApiResponse<>(0, thresholdConfig, "success");
    }

    @PostMapping("/change_threshold")
    public ApiResponse<Void> changeThreshold(@RequestBody ThresholdConfig newThresholdConfig) {
        thresholdConfig.setCpuThreshold(newThresholdConfig.getCpuThreshold());
        thresholdConfig.setMemoryThreshold(newThresholdConfig.getMemoryThreshold());
        thresholdConfig.setStorageThreshold(newThresholdConfig.getStorageThreshold());
        return new ApiResponse<>(0, null, "Threshold updated successfully");
    }

    @GetMapping("/threshold_alert")
    public ApiResponse<Map<String, Object>> getThresholdAlerts(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "30") int size,
                                                               @RequestParam(required = false) String alertType) {
        Page<ThresholdAlert> thresholdAlerts = systemMonitorService.getThresholdAlerts(alertType, page, size);

        Map<String, Object> data = Map.of(
                "totalPages", thresholdAlerts.getTotalPages(),
                "totalElements", thresholdAlerts.getTotalElements(),
                "content", thresholdAlerts.getContent()
        );

        return new ApiResponse<>(0, data, "success");
    }
}
