//package com.pennet.defender.service;
//
//import com.pennet.defender.model.FirewallRule;
//import com.pennet.defender.model.FirewallStatus;
//import com.pennet.defender.repository.FirewallRuleRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Service
//public class FirewallManagementService {
//
//    @Autowired
//    private FirewallRuleRepository firewallRuleRepository;
//
//    public Page<FirewallRule> getFirewallRules(int page, int size) {
//        Pageable pageable = PageRequest.of(page - 1, size);
//        return firewallRuleRepository.findAllByOrderByPriorityAsc(pageable);
//    }
//
//    public List<FirewallRule> getRulesByAction(String action) {
//        return firewallRuleRepository.findByActionOrderByPriorityAsc(action);
//    }
//
//    public FirewallStatus getFirewallStatus() throws IOException {
//        boolean firewallActive = isFirewalldActive();
//        boolean iptablesActive = isIptablesActive();
//
//        return new FirewallStatus(firewallActive, iptablesActive);
//    }
//
//    public void enableFirewall() throws IOException {
//        executeCommand("systemctl start firewalld");
//    }
//
//    public void disableFirewall() throws IOException {
//        executeCommand("systemctl stop firewalld");
//    }
//
//    public void updateFirewallRule(FirewallRule rule) throws IOException {
//        // First delete the existing rule if it exists
//        if (rule.getId() != null) {
//            FirewallRule existingRule = firewallRuleRepository.findById(rule.getId()).orElse(null);
//            if (existingRule != null) {
//                // We need to recreate the iptables command for the existing rule to delete it
//                String deleteCommand = createIptablesCommand(existingRule, true);
//                executeCommand(deleteCommand);
//            }
//        }
//
//        // Create and execute the command to add the new rule
//        String addCommand = createIptablesCommand(rule, false);
//        executeCommand(addCommand);
//
//        // Update rule in database
//        rule.setLastUpdated(LocalDateTime.now());
//        firewallRuleRepository.save(rule);
//
//        // Apply changes
//        saveIptablesRules();
//    }
//
//    public void refreshFirewallRules() throws IOException {
//        // Clear existing rules
//        firewallRuleRepository.deleteAll();
//
//        List<FirewallRule> rules = new ArrayList<>();
//        Process process = Runtime.getRuntime().exec("iptables -L -n -v");
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//            String line;
//            String currentChain = null;
//            int priority = 0;
//
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("Chain")) {
//                    Pattern chainPattern = Pattern.compile("Chain\\s+(\\w+)");
//                    Matcher chainMatcher = chainPattern.matcher(line);
//                    if (chainMatcher.find()) {
//                        currentChain = chainMatcher.group(1);
//                        priority = 0;
//                    }
//                } else if (line.matches("^\\s*\\d+.*") && currentChain != null) {
//                    // This is a rule line
//                    priority++;
//                    String[] parts = line.trim().split("\\s+");
//                    if (parts.length >= 3) {
//                        FirewallRule rule = new FirewallRule();
//                        rule.setChain(currentChain);
//                        rule.setAction(parts[2]); // Target (ACCEPT, DROP, etc)
//
//                        if (parts.length >= 4) {
//                            rule.setProtocol(parts[3]);
//                        }
//
//                        if (parts.length >= 7) {
//                            rule.setSource(parts[7]);
//                        }
//
//                        if (parts.length >= 8) {
//                            rule.setDestination(parts[8]);
//                        }
//
//                        // Try to find port information
//                        for (int i = 0; i < parts.length; i++) {
//                            if (parts[i].equals("dpt:") && i + 1 < parts.length) {
//                                rule.setPort(parts[i + 1]);
//                                break;
//                            }
//                        }
//
//                        rule.setPriority(priority);
//                        rule.setLastUpdated(LocalDateTime.now());
//                        rules.add(rule);
//                    }
//                }
//            }
//        }
//
//        firewallRuleRepository.saveAll(rules);
//    }
//
//    private boolean isFirewalldActive() throws IOException {
//        Process process = Runtime.getRuntime().exec("systemctl is-active firewalld");
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//            String result = reader.readLine();
//            return "active".equals(result);
//        }
//    }
//
//    private boolean isIptablesActive() throws IOException {
//        Process process = Runtime.getRuntime().exec("iptables -L -n");
//        try {
//            int exitCode = process.waitFor();
//            return exitCode == 0;
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            return false;
//        }
//    }
//
//    private void saveIptablesRules() throws IOException {
//        executeCommand("iptables-save > /etc/iptables/rules.v4");
//        executeCommand("systemctl restart firewalld");
//    }
//
//    private String createIptablesCommand(FirewallRule rule, boolean isDelete) {
//        StringBuilder command = new StringBuilder("iptables ");
//        command.append(isDelete ? "-D " : "-A ");
//        command.append(rule.getChain()).append(" ");
//
//        if (rule.getProtocol() != null && !rule.getProtocol().isEmpty()) {
//            command.append("-p ").append(rule.getProtocol()).append(" ");
//        }
//
//        if (rule.getSource() != null && !rule.getSource().isEmpty() && !rule.getSource().equals("0.0.0.0/0")) {
//            command.append("-s ").append(rule.getSource()).append(" ");
//        }
//
//        if (rule.getDestination() != null && !rule.getDestination().isEmpty() && !rule.getDestination().equals("0.0.0.0/0")) {
//            command.append("-d ").append(rule.getDestination()).append(" ");
//        }
//
//        if (rule.getPort() != null && !rule.getPort().isEmpty()) {
//            command.append("--dport ").append(rule.getPort()).append(" ");
//        }
//
//        command.append("-j ").append(rule.getAction());
//
//        return command.toString();
//    }
//
//    private void executeCommand(String command) throws IOException {
//        Process process = Runtime.getRuntime().exec(command);
//        try {
//            process.waitFor();
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//}
package com.pennet.defender.service;

import com.pennet.defender.model.FirewallRule;
import com.pennet.defender.model.FirewallStatus;
import com.pennet.defender.repository.FirewallRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FirewallManagementService {

    @Autowired
    private FirewallRuleRepository firewallRuleRepository;

    private static final List<String> ALLOWED_CHAINS = List.of("INPUT", "FORWARD", "OUTPUT");
    private static final List<String> ALLOWED_ACTIONS = List.of("ACCEPT", "REJECT", "DROP");

    public Page<FirewallRule> getFirewallRules(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return firewallRuleRepository.findAllByOrderByPriorityAsc(pageable);
    }

    public List<FirewallRule> getRulesByChain(String chain) {
        if (!ALLOWED_CHAINS.contains(chain)) {
            throw new IllegalArgumentException("Invalid chain: " + chain);
        }
        return firewallRuleRepository.findByChainOrderByPriorityAsc(chain);
    }

    public List<FirewallRule> getRulesByAction(String action) {
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Invalid action: " + action);
        }
        return firewallRuleRepository.findByActionOrderByPriorityAsc(action);
    }

    public FirewallStatus getFirewallStatus() throws IOException {
        boolean iptablesActive = isIptablesActive();
        return new FirewallStatus(iptablesActive, iptablesActive);
    }

    public FirewallRule addFirewallRule(FirewallRule rule) throws IOException {
        validateRule(rule);

        // Create and execute the command to add the new rule
        String addCommand = createIptablesCommand(rule, false);
        executeCommand(addCommand);

        // Set rule properties
        rule.setLastUpdated(LocalDateTime.now());
        if (rule.getPriority() == 0) {
            // Assign a priority if not set
            int highestPriority = firewallRuleRepository.findMaxPriorityByChain(rule.getChain());
            rule.setPriority(highestPriority + 1);
        }

        // Save rule to database
        FirewallRule savedRule = firewallRuleRepository.save(rule);

        // Apply changes
        saveIptablesRules();

        return savedRule;
    }

    public void updateFirewallRule(FirewallRule rule) throws IOException {
        validateRule(rule);

        // First delete the existing rule if it exists
        if (rule.getId() != null) {
            FirewallRule existingRule = firewallRuleRepository.findById(rule.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Rule not found with ID: " + rule.getId()));

            // Delete the existing rule
            String deleteCommand = createIptablesCommand(existingRule, true);
            executeCommand(deleteCommand);
        }

        // Create and execute the command to add the updated rule
        String addCommand = createIptablesCommand(rule, false);
        executeCommand(addCommand);

        // Update rule in database
        rule.setLastUpdated(LocalDateTime.now());
        firewallRuleRepository.save(rule);

        // Apply changes
        saveIptablesRules();
    }

    public void deleteFirewallRule(Integer ruleId) throws IOException {
        FirewallRule rule = firewallRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found with ID: " + ruleId));

        // Delete the rule from iptables
        String deleteCommand = createIptablesCommand(rule, true);
        executeCommand(deleteCommand);

        // Delete from database
        firewallRuleRepository.deleteById(ruleId);

        // Apply changes
        saveIptablesRules();
    }

    public void refreshFirewallRules() throws IOException {
        // Clear existing rules
        firewallRuleRepository.deleteAll();

        List<FirewallRule> rules = new ArrayList<>();

        // Only focus on filter table
        Process process = Runtime.getRuntime().exec("iptables -t filter -L -n -v --line-numbers");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            String currentChain = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Chain")) {
                    Pattern chainPattern = Pattern.compile("Chain\\s+(\\w+)");
                    Matcher chainMatcher = chainPattern.matcher(line);
                    if (chainMatcher.find()) {
                        currentChain = chainMatcher.group(1);
                        // Only process the three chains we care about
                        if (!ALLOWED_CHAINS.contains(currentChain)) {
                            currentChain = null;
                        }
                    }
                } else if (line.matches("^\\s*\\d+.*") && currentChain != null) {
                    // This is a rule line
                    FirewallRule rule = parseIptablesRuleLine(line, currentChain);
                    if (rule != null) {
                        rules.add(rule);
                    }
                }
            }
        }

        firewallRuleRepository.saveAll(rules);
    }

    private FirewallRule parseIptablesRuleLine(String line, String chain) {
        // Line number/priority comes first
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 4) return null; // Not enough parts for a valid rule

        FirewallRule rule = new FirewallRule();
        rule.setChain(chain);

        try {
            // Line number is the priority
            rule.setPriority(Integer.parseInt(parts[0]));

            // Find action (target)
            rule.setAction(parts[2]); // Target (ACCEPT, DROP, REJECT)

            // Protocol
            if (parts.length > 3 && !parts[3].equals("all")) {
                rule.setProtocol(parts[3]);
            } else {
                rule.setProtocol("all");
            }

            // Source and destination
            for (int i = 0; i < parts.length; i++) {
                if (i + 1 < parts.length) {
                    // Source address
                    if (parts[i].equals("source") || parts[i].equals("src")) {
                        rule.setSource(parts[i+1]);
                    }
                    // Destination address
                    else if (parts[i].equals("destination") || parts[i].equals("dst")) {
                        rule.setDestination(parts[i+1]);
                    }
                    // Port
                    else if (parts[i].equals("dpt:")) {
                        rule.setPort(parts[i+1]);
                    }
                }
            }

            rule.setLastUpdated(LocalDateTime.now());
            return rule;
        } catch (NumberFormatException e) {
            return null; // Couldn't parse priority
        }
    }

    private boolean isIptablesActive() throws IOException {
        Process process = Runtime.getRuntime().exec("iptables -L -n");
        try {
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void saveIptablesRules() throws IOException {
        executeCommand("iptables-save > /etc/iptables/rules.v4");
    }

    private String createIptablesCommand(FirewallRule rule, boolean isDelete) {
        StringBuilder command = new StringBuilder("iptables -t filter ");

        if (isDelete) {
            command.append("-D ");
        } else {
            command.append("-A ");
        }

        command.append(rule.getChain()).append(" ");

        if (rule.getProtocol() != null && !rule.getProtocol().isEmpty() && !rule.getProtocol().equals("all")) {
            command.append("-p ").append(rule.getProtocol()).append(" ");
        }

        if (rule.getSource() != null && !rule.getSource().isEmpty() && !rule.getSource().equals("0.0.0.0/0")) {
            command.append("-s ").append(rule.getSource()).append(" ");
        }

        if (rule.getDestination() != null && !rule.getDestination().isEmpty() && !rule.getDestination().equals("0.0.0.0/0")) {
            command.append("-d ").append(rule.getDestination()).append(" ");
        }

        if (rule.getPort() != null && !rule.getPort().isEmpty()) {
            command.append("--dport ").append(rule.getPort()).append(" ");
        }

        command.append("-j ").append(rule.getAction());

        return command.toString();
    }

    private void validateRule(FirewallRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        if (rule.getChain() == null || !ALLOWED_CHAINS.contains(rule.getChain())) {
            throw new IllegalArgumentException("Invalid chain. Must be one of: " + ALLOWED_CHAINS);
        }

        if (rule.getAction() == null || !ALLOWED_ACTIONS.contains(rule.getAction())) {
            throw new IllegalArgumentException("Invalid action. Must be one of: " + ALLOWED_ACTIONS);
        }
    }

    private void executeCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                }
                throw new IOException("Command failed with exit code " + exitCode + ": " + command +
                        "\nError: " + errorOutput);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command execution interrupted: " + command, e);
        }
    }
}