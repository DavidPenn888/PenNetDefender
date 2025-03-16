package com.pennet.defender.service;

import com.pennet.defender.model.FirewallRule;
import com.pennet.defender.model.FirewallStatus;
import com.pennet.defender.repository.FirewallRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FirewallManagementService {
    private static final Logger logger = LoggerFactory.getLogger(FirewallManagementService.class);

    @Autowired
    private FirewallRuleRepository firewallRuleRepository;

    @Autowired
    private IptablesService iptablesService;

    /**
     * 获取防火墙状态
     */
    public FirewallStatus getFirewallStatus() throws IOException {
        return iptablesService.getFirewallStatus();
    }

    // 防火墙启用和禁用功能已被移除

    /**
     * 获取防火墙规则（分页）
     */
    public Page<FirewallRule> getFirewallRules(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return firewallRuleRepository.findAllByOrderByPriorityAsc(pageable);
    }

    /**
     * 添加防火墙规则
     */
    @Transactional
    public FirewallRule addFirewallRule(FirewallRule rule) throws IOException {
        // 设置优先级
        if (rule.getPriority() == null) {
            int maxPriority = firewallRuleRepository.findMaxPriorityByChain(rule.getChain());
            rule.setPriority(maxPriority + 1);
        }

        rule.setLastUpdated(LocalDateTime.now());

        // 保存到数据库
        FirewallRule savedRule = firewallRuleRepository.save(rule);

        // 应用到iptables
        iptablesService.addRule(rule);

        return savedRule;
    }

    /**
     * 更新防火墙规则
     */
    @Transactional
    public void updateFirewallRule(FirewallRule rule) throws IOException {
        Optional<FirewallRule> existingRuleOpt = firewallRuleRepository.findById(rule.getId());

        if (existingRuleOpt.isPresent()) {
            FirewallRule existingRule = existingRuleOpt.get();

            // 删除旧规则
            iptablesService.deleteRule(existingRule);

            // 更新规则属性
            existingRule.setChain(rule.getChain());
            existingRule.setAction(rule.getAction());
            existingRule.setProtocol(rule.getProtocol());
            existingRule.setSource(rule.getSource());
            existingRule.setDestination(rule.getDestination());
            existingRule.setPort(rule.getPort());
            // 更新优先级（如果前端提供了新的优先级）
            if (rule.getPriority() != null) {
                existingRule.setPriority(rule.getPriority());
            }
            existingRule.setLastUpdated(LocalDateTime.now());

            // 保存到数据库
            firewallRuleRepository.save(existingRule);

            // 添加新规则
            iptablesService.addRule(existingRule);
        } else {
            throw new IOException("Rule not found with ID: " + rule.getId());
        }
    }

    /**
     * 删除防火墙规则
     */
    @Transactional
    public void deleteFirewallRule(Integer id) throws IOException {
        Optional<FirewallRule> ruleOpt = firewallRuleRepository.findById(id);

        if (ruleOpt.isPresent()) {
            FirewallRule rule = ruleOpt.get();

            // 从iptables删除
            iptablesService.deleteRule(rule);

            // 从数据库删除
            firewallRuleRepository.deleteById(id);
        } else {
            throw new IOException("Rule not found with ID: " + id);
        }
    }

    /**
     * 刷新防火墙规则（从iptables同步到数据库）
     */
    @Transactional
    public void refreshFirewallRules() throws IOException {
        // 获取iptables中的规则
        List<FirewallRule> iptablesRules = iptablesService.parseIptablesRules();

        // 清空数据库中的规则
        firewallRuleRepository.deleteAll();

        // 保存新规则到数据库
        for (FirewallRule rule : iptablesRules) {
            rule.setLastUpdated(LocalDateTime.now());
            firewallRuleRepository.save(rule);
        }
    }

    /**
     * 应用数据库中的规则到iptables
     */
    private void applyRulesFromDatabase() throws IOException, InterruptedException {
        // 清除现有规则 - 使用直接执行iptables命令的方式替代disableFirewall()
        // 清空所有规则
        Process process1 = Runtime.getRuntime().exec("iptables -F");
        process1.waitFor();
        // 设置默认策略为ACCEPT
        Process process2 = Runtime.getRuntime().exec("iptables -P INPUT ACCEPT");
        process2.waitFor();
        Process process3 = Runtime.getRuntime().exec("iptables -P OUTPUT ACCEPT");
        process3.waitFor();
        Process process4 = Runtime.getRuntime().exec("iptables -P FORWARD ACCEPT");
        process4.waitFor();

        // 获取数据库中的所有规则
        List<FirewallRule> rules = firewallRuleRepository.findAll();

        // 按优先级排序并应用
        rules.stream()
                .sorted((r1, r2) -> r1.getPriority().compareTo(r2.getPriority()))
                .forEach(rule -> {
                    try {
                        iptablesService.addRule(rule);
                    } catch (IOException e) {
                        logger.error("Failed to apply rule: {}", rule.getId(), e);
                    }
                });
    }
}