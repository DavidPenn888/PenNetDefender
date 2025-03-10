package com.pennet.defender.service;

import com.pennet.defender.model.FirewallRule;
import com.pennet.defender.model.FirewallStatus;
import com.pennet.defender.repository.FirewallRuleRepository;
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

    @Autowired
    private FirewallRuleRepository firewallRuleRepository;

    @Autowired
    private IptablesService iptablesService;

    // 获取防火墙状态
    public FirewallStatus getFirewallStatus() throws IOException {
        FirewallStatus status = new FirewallStatus();
        status.setEnabled(iptablesService.isFirewallEnabled());

        // 获取各链的规则数量
        long totalRules = firewallRuleRepository.count();
        long inputRules = firewallRuleRepository.countByChain("INPUT");
        long outputRules = firewallRuleRepository.countByChain("OUTPUT");
        long forwardRules = firewallRuleRepository.countByChain("FORWARD");

        status.setTotalRules((int)totalRules);
        status.setInputRules((int)inputRules);
        status.setOutputRules((int)outputRules);
        status.setForwardRules((int)forwardRules);

        return status;
    }

    // 启用防火墙
    public void enableFirewall() throws IOException {
        iptablesService.enableFirewall();
        refreshFirewallRules();
    }

    // 禁用防火墙
    public void disableFirewall() throws IOException {
        iptablesService.disableFirewall();
        refreshFirewallRules();
    }

    // 获取防火墙规则（分页）
    public Page<FirewallRule> getFirewallRules(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return firewallRuleRepository.findAllByOrderByPriorityAsc(pageable);
    }

    // 添加防火墙规则
    @Transactional
    public FirewallRule addFirewallRule(FirewallRule rule) throws IOException {
        // 设置优先级
        if (rule.getPriority() == null) {
            int maxPriority = firewallRuleRepository.findMaxPriorityByChain(rule.getChain());
            rule.setPriority(maxPriority + 1);
        }

        // 设置更新时间
        rule.setLastUpdated(LocalDateTime.now());

        // 保存到数据库
        FirewallRule savedRule = firewallRuleRepository.save(rule);

        // 应用到iptables
        iptablesService.addRule(savedRule);

        return savedRule;
    }

    // 更新防火墙规则
    @Transactional
    public void updateFirewallRule(FirewallRule rule) throws IOException {
        Optional<FirewallRule> existingRuleOpt = firewallRuleRepository.findById(rule.getId());

        if (existingRuleOpt.isPresent()) {
            FirewallRule existingRule = existingRuleOpt.get();

            // 更新iptables规则
            iptablesService.updateRule(existingRule, rule);

            // 更新数据库中的规则
            rule.setLastUpdated(LocalDateTime.now());
            firewallRuleRepository.save(rule);
        } else {
            throw new IOException("Rule not found with ID: " + rule.getId());
        }
    }

    // 删除防火墙规则
    @Transactional
    public void deleteFirewallRule(Integer id) throws IOException {
        Optional<FirewallRule> ruleOpt = firewallRuleRepository.findById(id);

        if (ruleOpt.isPresent()) {
            FirewallRule rule = ruleOpt.get();

            // 从iptables中删除规则
            iptablesService.deleteRule(rule);

            // 从数据库中删除规则
            firewallRuleRepository.deleteById(id);
        } else {
            throw new IOException("Rule not found with ID: " + id);
        }
    }

    // 刷新防火墙规则（从iptables同步到数据库）
    @Transactional
    public void refreshFirewallRules() throws IOException {
        // 获取iptables中的规则
        List<FirewallRule> iptablesRules = iptablesService.parseIptablesRules();

        // 清空数据库中的规则
        firewallRuleRepository.deleteAll();

        // 将iptables规则保存到数据库
        for (FirewallRule rule : iptablesRules) {
            rule.setLastUpdated(LocalDateTime.now());
            firewallRuleRepository.save(rule);
        }
    }
}
