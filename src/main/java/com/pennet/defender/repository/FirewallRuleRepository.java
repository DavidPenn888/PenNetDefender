package com.pennet.defender.repository;

import com.pennet.defender.model.FirewallRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FirewallRuleRepository extends JpaRepository<FirewallRule, Integer> {
    Page<FirewallRule> findAllByOrderByPriorityAsc(Pageable pageable);
    List<FirewallRule> findByActionOrderByPriorityAsc(String action);
}