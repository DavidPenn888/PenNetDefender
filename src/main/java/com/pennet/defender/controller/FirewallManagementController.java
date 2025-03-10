//package com.pennet.defender.controller;
//
//import com.pennet.defender.model.FirewallRule;
//import com.pennet.defender.model.FirewallStatus;
//import com.pennet.defender.service.FirewallManagementService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/firewall")
//public class FirewallManagementController {
//
//    @Autowired
//    private FirewallManagementService firewallManagementService;
//
//    @GetMapping("/status")
//    public FirewallStatus getFirewallStatus() throws IOException {
//        return firewallManagementService.getFirewallStatus();
//    }
//
//    @PostMapping("/enable")
//    public Map<String, String> enableFirewall() throws IOException {
//        firewallManagementService.enableFirewall();
//        return Map.of("status", "Firewall enabled");
//    }
//
//    @PostMapping("/disable")
//    public Map<String, String> disableFirewall() throws IOException {
//        firewallManagementService.disableFirewall();
//        return Map.of("status", "Firewall disabled");
//    }
//
//    @GetMapping("/rules")
//    public Map<String, Object> listFirewallRules(@RequestParam(defaultValue = "1") int page,
//                                                 @RequestParam(defaultValue = "30") int size) throws IOException {
//        firewallManagementService.refreshFirewallRules();
//        Page<FirewallRule> rules = firewallManagementService.getFirewallRules(page, size);
//
//        return Map.of(
//                "totalPages", rules.getTotalPages(),
//                "totalElements", rules.getTotalElements(),
//                "content", rules.getContent()
//        );
//    }
//
//    @GetMapping("/rules/{action}")
//    public List<FirewallRule> getRulesByAction(@PathVariable String action) throws IOException {
//        firewallManagementService.refreshFirewallRules();
//        return firewallManagementService.getRulesByAction(action);
//    }
//
//    @PostMapping("/rules/update")
//    public Map<String, String> updateFirewallRule(@RequestBody FirewallRule rule) throws IOException {
//        firewallManagementService.updateFirewallRule(rule);
//        return Map.of("status", "Firewall rule updated");
//    }
//}

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

    @GetMapping("/rules")
    public Map<String, Object> listFirewallRules(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "30") int size) throws IOException {
        firewallManagementService.refreshFirewallRules();
        Page<FirewallRule> rules = firewallManagementService.getFirewallRules(page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("totalPages", rules.getTotalPages());
        response.put("totalElements", rules.getTotalElements());
        response.put("currentPage", page);
        response.put("pageSize", size);
        response.put("content", rules.getContent());

        return response;
    }

    @GetMapping("/rules/action/{action}")
    public List<FirewallRule> getRulesByAction(@PathVariable String action) throws IOException {
        firewallManagementService.refreshFirewallRules();
        return firewallManagementService.getRulesByAction(action);
    }

    @GetMapping("/rules/chain/{chain}")
    public List<FirewallRule> getRulesByChain(@PathVariable String chain) throws IOException {
        firewallManagementService.refreshFirewallRules();
        return firewallManagementService.getRulesByChain(chain);
    }

    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> addFirewallRule(@RequestBody FirewallRule rule) throws IOException {
        FirewallRule savedRule = firewallManagementService.addFirewallRule(rule);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Firewall rule added successfully");
        response.put("rule", savedRule);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> updateFirewallRule(
            @PathVariable Integer id,
            @RequestBody FirewallRule rule) throws IOException {

        rule.setId(id);
        firewallManagementService.updateFirewallRule(rule);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Firewall rule updated successfully");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> deleteFirewallRule(@PathVariable Integer id) throws IOException {
        firewallManagementService.deleteFirewallRule(id);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Firewall rule deleted successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshRules() throws IOException {
        firewallManagementService.refreshFirewallRules();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Firewall rules refreshed successfully");

        return ResponseEntity.ok(response);
    }
}