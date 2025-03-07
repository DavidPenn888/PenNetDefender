package com.pennet.defender.controller;

import com.pennet.defender.model.AppService;
import com.pennet.defender.service.ServiceManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/appservice")
public class AppServiceManagementController {

    @Autowired
    private ServiceManagementService serviceManagementService;

    @GetMapping("/list")
    public Map<String, Object> listServices(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "30") int size) throws IOException {
        serviceManagementService.refreshServices();
        Page<AppService> appservices = serviceManagementService.getServices(page, size);

        return Map.of(
                "totalPages", appservices.getTotalPages(),
                "totalElements", appservices.getTotalElements(),
                "content", appservices.getContent()
        );
    }

    @PostMapping("/start/{serviceName}")
    public Map<String, String> startService(@PathVariable String serviceName) throws IOException {
        serviceManagementService.startService(serviceName);
        return Map.of("status", "Service " + serviceName + " started");
    }

    @PostMapping("/stop/{serviceName}")
    public Map<String, String> stopService(@PathVariable String serviceName) throws IOException {
        serviceManagementService.stopService(serviceName);
        return Map.of("status", "Service " + serviceName + " stopped");
    }

    @PostMapping("/restart/{serviceName}")
    public Map<String, String> restartService(@PathVariable String serviceName) throws IOException {
        serviceManagementService.restartService(serviceName);
        return Map.of("status", "Service " + serviceName + " restarted");
    }

    @PostMapping("/enable/{serviceName}")
    public Map<String, String> enableService(@PathVariable String serviceName) throws IOException {
        serviceManagementService.enableService(serviceName);
        return Map.of("status", "Service " + serviceName + " enabled for auto-start");
    }

    @PostMapping("/disable/{serviceName}")
    public Map<String, String> disableService(@PathVariable String serviceName) throws IOException {
        serviceManagementService.disableService(serviceName);
        return Map.of("status", "Service " + serviceName + " disabled for auto-start");
    }
}
