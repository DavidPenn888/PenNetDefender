package com.pennet.defender.controller;

import com.pennet.defender.model.FirewallRule;
import com.pennet.defender.model.FirewallStatus;
import com.pennet.defender.service.FirewallManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/firewall")
public class FirewallManagementController {

    @Autowired
    private FirewallManagementService firewallManagementService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFirewallStatus() {
        try {
            FirewallStatus status = firewallManagementService.getFirewallStatus();
            return ResponseEntity.ok(Map.of("code", 0, "data", status, "message", "success"));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("code", -1, "data", new HashMap<>(), "message", e.getMessage()));
        }
    }

    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enableFirewall() {
        try {
            firewallManagementService.enableFirewall();
            return ResponseEntity.ok(Map.of("code", 0, "data", "Firewall enabled", "message", "success"));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("code", -1, "data", new HashMap<>(), "message", e.getMessage()));
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableFirewall() {
        try {
            firewallManagementService.disableFirewall();
            return ResponseEntity.ok(Map.of("code", 0, "data", "Firewall disabled", "message", "success"));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("code", -1, "data", new HashMap<>(), "message", e.getMessage()));
        }
    }

    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> listFirewallRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            firewallManagementService.refreshFirewallRules();
            Page<FirewallRule> rules = firewallManagementService.getFirewallRules(page, size);

            Map<String, Object> data = new HashMap<>();
            data.put("totalPages", rules.getTotalPages());
            data.put("totalElements", rules.getTotalElements());
            data.put("currentPage", page);
            data.put("pageSize", size);
            data.put("content", rules.getContent());

            return ResponseEntity.ok(Map.of("code", 0, "data", data, "message", "success"));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("code", -1, "data", new HashMap<>(), "message", e.getMessage()));
        }
    }

    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> addFirewallRule(@RequestBody FirewallRule rule) {
        try {
            FirewallRule savedRule = firewallManagementService.addFirewallRule(rule);
            return ResponseEntity.ok(Map.of("code", 0, "data", savedRule, "message", "Firewall rule added successfully"));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("code", -1, "data", new HashMap<>(), "message", e.getMessage()));
        }
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> updateFirewallRule(
            @PathVariable Integer id,
            @RequestBody FirewallRule rule) {
        try {
            rule.setId(id);
            firewallManagementService.updateFirewallRule(rule);
            return ResponseEntity.ok(Map.of("code", 0, "data", new HashMap<>(), "message", "Firewall rule updated successfully"));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("code", -1, "data", new HashMap<>(), "message", e.getMessage()));
        }
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> deleteFirewallRule(@PathVariable Integer id) {
        try {
            firewallManagementService.deleteFirewallRule(id);
            return ResponseEntity.ok(Map.of("code", 0, "data", new HashMap<>(), "message", "Firewall rule deleted successfully"));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("code", -1, "data", new HashMap<>(), "message", e.getMessage()));
        }
    }

    @GetMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshRules() {
        try {
            firewallManagementService.refreshFirewallRules();
            return ResponseEntity.ok(Map.of("code", 0, "data", new HashMap<>(), "message", "Firewall rules refreshed successfully"));
        } catch (IOException e) {
            return ResponseEntity.ok(Map.of("code", -1, "data", new HashMap<>(), "message", e.getMessage()));
        }
    }
}
