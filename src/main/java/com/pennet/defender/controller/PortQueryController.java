package com.pennet.defender.controller;

import com.pennet.defender.model.Port;
import com.pennet.defender.service.PortQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/port")
public class PortQueryController {

    @Autowired
    private PortQueryService portQueryService;

    @GetMapping("/list")
    public Map<String, Object> listPorts(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "30") int size) throws IOException {
        portQueryService.refreshPorts();
        Page<Port> ports = portQueryService.getPorts(page, size);

        return Map.of(
                "totalPages", ports.getTotalPages(),
                "totalElements", ports.getTotalElements(),
                "content", ports.getContent()
        );
    }
}