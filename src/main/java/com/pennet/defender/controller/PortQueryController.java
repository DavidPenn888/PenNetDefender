package com.pennet.defender.controller;

import com.pennet.defender.model.ApiResponse;
import com.pennet.defender.model.Port;
import com.pennet.defender.service.PortQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/port")
public class PortQueryController {

    @Autowired
    private PortQueryService portQueryService;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> listPorts(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "30") int size,
                                                      @RequestParam(required = false) String queryType,
                                                      @RequestParam(required = false) String queryValue) throws IOException {
        portQueryService.refreshPorts();
        Page<Port> ports;
        
        if (queryType != null && queryValue != null && !queryValue.trim().isEmpty()) {
            switch (queryType) {
                case "portNumber":
                    try {
                        Integer portNumber = Integer.parseInt(queryValue);
                        ports = portQueryService.getPortsByPortNumber(portNumber, page, size);
                    } catch (NumberFormatException e) {
                        return new ApiResponse<>(-1, null, "端口号必须是数字");
                    }
                    break;
                case "processId":
                    try {
                        Integer processId = Integer.parseInt(queryValue);
                        ports = portQueryService.getPortsByProcessId(processId, page, size);
                    } catch (NumberFormatException e) {
                        return new ApiResponse<>(-1, null, "进程ID必须是数字");
                    }
                    break;
                case "childProcessId":
                    ports = portQueryService.getPortsByChildProcessId(queryValue, page, size);
                    break;
                default:
                    ports = portQueryService.getPorts(page, size);
            }
        } else {
            ports = portQueryService.getPorts(page, size);
        }

        Map<String, Object> data = Map.of(
                "totalPages", ports.getTotalPages(),
                "totalElements", ports.getTotalElements(),
                "content", ports.getContent()
        );

        return new ApiResponse<>(0, data, "success");
    }

    // POST /kill to terminate the process normally
    @PostMapping("/kill")
    public ApiResponse<String> killProcess(@RequestParam int pid) {
        String result = portQueryService.killProcess(pid);
        if (result.startsWith("Error")) {
            return new ApiResponse<>(-1, result, "error");
        }
        return new ApiResponse<>(0, result, "success");
    }

    // POST /forcekill to forcefully terminate the process using kill -9
    @PostMapping("/forcekill")
    public ApiResponse<String> forceKillProcess(@RequestParam int pid) {
        String result = portQueryService.forceKillProcess(pid);
        if (result.startsWith("Error")) {
            return new ApiResponse<>(-1, result, "error");
        }
        return new ApiResponse<>(0, result, "success");
    }

}
