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
                                                      @RequestParam(defaultValue = "30") int size) throws IOException {
        portQueryService.refreshPorts();
        Page<Port> ports = portQueryService.getPorts(page, size);

        Map<String, Object> data = Map.of(
                "totalPages", ports.getTotalPages(),
                "totalElements", ports.getTotalElements(),
                "content", ports.getContent()
        );

        return new ApiResponse<>(0, data, "success");
    }

//    // POST /kill to terminate the process normally
//    @PostMapping("/kill")
//    public ApiResponse<String> killProcess(@RequestParam int pid) {
//        try {
//            // Execute the kill command for the given PID
//            String command = "kill " + pid;
//            Process process = Runtime.getRuntime().exec(command);
//            process.waitFor();  // Wait for the command to finish
//
//            return new ApiResponse<>(0, "Process with PID " + pid + " has been terminated", "success");
//        } catch (IOException | InterruptedException e) {
//            return new ApiResponse<>(-1, e.getMessage(), "error");
//        }
//    }
//
//    // POST /forcekill to forcefully terminate the process using kill -9
//    @PostMapping("/forcekill")
//    public ApiResponse<String> forceKillProcess(@RequestParam int pid) {
//        try {
//            // Execute the kill -9 command for the given PID
//            String command = "kill -9 " + pid;
//            Process process = Runtime.getRuntime().exec(command);
//            process.waitFor();  // Wait for the command to finish
//
//            return new ApiResponse<>(0, "Process with PID " + pid + " has been forcefully terminated", "success");
//        } catch (IOException | InterruptedException e) {
//            return new ApiResponse<>(-1, e.getMessage(), "error");
//        }
//    }

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
