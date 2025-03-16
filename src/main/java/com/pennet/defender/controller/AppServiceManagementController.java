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
                                            @RequestParam(defaultValue = "30") int size,
                                            @RequestParam(required = false) String name) {
        try {
            serviceManagementService.refreshServices();
            Page<AppService> appservices;
            
            if (name != null && !name.trim().isEmpty()) {
                // 如果提供了服务名称，则按名称查询
                appservices = serviceManagementService.searchServicesByName(name, page, size);
            } else {
                // 否则获取所有服务
                appservices = serviceManagementService.getServices(page, size);
            }
            
            return Map.of("code", 0, "data", Map.of(
                    "totalPages", appservices.getTotalPages(),
                    "totalElements", appservices.getTotalElements(),
                    "content", appservices.getContent()
            ), "message", "success");
        } catch (IOException e) {
            return Map.of("code", -1, "data", Map.of(), "message", e.getMessage());
        }
    }

//    @PostMapping("/action/{serviceName}")
//    public Map<String, Object> manageService(@PathVariable String serviceName, @RequestParam String action) {
//        switch (action.toLowerCase()) {
//            case "start":
//                return executeServiceCommand(() -> serviceManagementService.startService(serviceName), "Service " + serviceName + " started");
//            case "stop":
//                return executeServiceCommand(() -> serviceManagementService.stopService(serviceName), "Service " + serviceName + " stopped");
//            case "restart":
//                return executeServiceCommand(() -> serviceManagementService.restartService(serviceName), "Service " + serviceName + " restarted");
//            case "enable":
//                return executeServiceCommand(() -> serviceManagementService.enableService(serviceName), "Service " + serviceName + " enabled for auto-start");
//            case "disable":
//                return executeServiceCommand(() -> serviceManagementService.disableService(serviceName), "Service " + serviceName + " disabled for auto-start");
//            default:
//                return Map.of("code", -1, "data", Map.of(), "message", "Invalid action: " + action);
//        }
//    }

    @PostMapping("/action")
    public Map<String, Object> manageService(@RequestParam String action, @RequestParam String servicename) {
        switch (action.toLowerCase()) {
            case "start":
                return executeServiceCommand(() -> serviceManagementService.startService(servicename), "Service " + servicename + " started");
            case "stop":
                return executeServiceCommand(() -> serviceManagementService.stopService(servicename), "Service " + servicename + " stopped");
            case "restart":
                return executeServiceCommand(() -> serviceManagementService.restartService(servicename), "Service " + servicename + " restarted");
            case "enable":
                return executeServiceCommand(() -> serviceManagementService.enableService(servicename), "Service " + servicename + " enabled for auto-start");
            case "disable":
                return executeServiceCommand(() -> serviceManagementService.disableService(servicename), "Service " + servicename + " disabled for auto-start");
            default:
                return Map.of("code", -1, "data", Map.of(), "message", "Invalid action: " + action);
        }
    }

    private Map<String, Object> executeServiceCommand(ServiceCommand command, String successMessage) {
        try {
            command.execute();
            return Map.of("code", 0, "data", Map.of(), "message", successMessage);
        } catch (IOException e) {
            return Map.of("code", -1, "data", Map.of(), "message", e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ServiceCommand {
        void execute() throws IOException;
    }
}
