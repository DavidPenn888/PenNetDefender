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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FirewallManagementService {

    @Autowired
    private FirewallRuleRepository firewallRuleRepository;

    public Page<FirewallRule> getFirewallRules(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return firewallRuleRepository.findAllByOrderByPriorityAsc(pageable);
    }

    public List<FirewallRule> getRulesByAction(String action) {
        return firewallRuleRepository.findByActionOrderByPriorityAsc(action);
    }

    public FirewallStatus getFirewallStatus() throws IOException {
        boolean firewallActive = isFirewalldActive();
        boolean iptablesActive = isIptablesActive();

        return new FirewallStatus(firewallActive, iptablesActive);
    }

    public void enableFirewall() throws IOException {
        executeCommand("systemctl start firewalld");
    }

    public void disableFirewall() throws IOException {
        executeCommand("systemctl stop firewalld");
    }

    public void updateFirewallRule(FirewallRule rule) throws IOException {
        // First delete the existing rule if it exists
        if (rule.getId() != null) {
            FirewallRule existingRule = firewallRuleRepository.findById(rule.getId()).orElse(null);
            if (existingRule != null) {
                // We need to recreate the iptables command for the existing rule to delete it
                String deleteCommand = createIptablesCommand(existingRule, true);
                executeCommand(deleteCommand);
            }
        }

        // Create and execute the command to add the new rule
        String addCommand = createIptablesCommand(rule, false);
        executeCommand(addCommand);

        // Update rule in database
        rule.setLastUpdated(LocalDateTime.now());
        firewallRuleRepository.save(rule);

        // Apply changes
        saveIptablesRules();
    }

    public void refreshFirewallRules() throws IOException {
        // Clear existing rules
        firewallRuleRepository.deleteAll();

        List<FirewallRule> rules = new ArrayList<>();
        Process process = Runtime.getRuntime().exec("iptables -L -n -v");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            String currentChain = null;
            int priority = 0;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Chain")) {
                    Pattern chainPattern = Pattern.compile("Chain\\s+(\\w+)");
                    Matcher chainMatcher = chainPattern.matcher(line);
                    if (chainMatcher.find()) {
                        currentChain = chainMatcher.group(1);
                        priority = 0;
                    }
                } else if (line.matches("^\\s*\\d+.*") && currentChain != null) {
                    // This is a rule line
                    priority++;
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 3) {
                        FirewallRule rule = new FirewallRule();
                        rule.setChain(currentChain);
                        rule.setAction(parts[2]); // Target (ACCEPT, DROP, etc)

                        if (parts.length >= 4) {
                            rule.setProtocol(parts[3]);
                        }

                        if (parts.length >= 7) {
                            rule.setSource(parts[7]);
                        }

                        if (parts.length >= 8) {
                            rule.setDestination(parts[8]);
                        }

                        // Try to find port information
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equals("dpt:") && i + 1 < parts.length) {
                                rule.setPort(parts[i + 1]);
                                break;
                            }
                        }

                        rule.setPriority(priority);
                        rule.setLastUpdated(LocalDateTime.now());
                        rules.add(rule);
                    }
                }
            }
        }

        firewallRuleRepository.saveAll(rules);
    }

    private boolean isFirewalldActive() throws IOException {
        Process process = Runtime.getRuntime().exec("systemctl is-active firewalld");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String result = reader.readLine();
            return "active".equals(result);
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
        executeCommand("systemctl restart firewalld");
    }

    private String createIptablesCommand(FirewallRule rule, boolean isDelete) {
        StringBuilder command = new StringBuilder("iptables ");
        command.append(isDelete ? "-D " : "-A ");
        command.append(rule.getChain()).append(" ");

        if (rule.getProtocol() != null && !rule.getProtocol().isEmpty()) {
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

    private void executeCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}