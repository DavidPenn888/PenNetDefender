package com.pennet.defender.controller;

import com.pennet.defender.model.FirewallRule;
import com.pennet.defender.model.FirewallStatus;
import com.pennet.defender.service.FirewallManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/firewall")
public class FirewallManagementController {

    @Autowired
    private FirewallManagementService firewallManagementService;

    @GetMapping("/status")
    public FirewallStatus getFirewallStatus() throws IOException {
        return firewallManagementService.getFirewallStatus();
    }

    @PostMapping("/enable")
    public Map<String, String> enableFirewall() throws IOException {
        firewallManagementService.enableFirewall();
        return Map.of("status", "Firewall enabled");
    }

    @PostMapping("/disable")
    public Map<String, String> disableFirewall() throws IOException {
        firewallManagementService.disableFirewall();
        return Map.of("status", "Firewall disabled");
    }

    @GetMapping("/rules")
    public Map<String, Object> listFirewallRules(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "30") int size) throws IOException {
        firewallManagementService.refreshFirewallRules();
        Page<FirewallRule> rules = firewallManagementService.getFirewallRules(page, size);

        return Map.of(
                "totalPages", rules.getTotalPages(),
                "totalElements", rules.getTotalElements(),
                "content", rules.getContent()
        );
    }

    @GetMapping("/rules/{action}")
    public List<FirewallRule> getRulesByAction(@PathVariable String action) throws IOException {
        firewallManagementService.refreshFirewallRules();
        return firewallManagementService.getRulesByAction(action);
    }

    @PostMapping("/rules/update")
    public Map<String, String> updateFirewallRule(@RequestBody FirewallRule rule) throws IOException {
        firewallManagementService.updateFirewallRule(rule);
        return Map.of("status", "Firewall rule updated");
    }
}
