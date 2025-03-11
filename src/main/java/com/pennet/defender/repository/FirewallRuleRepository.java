package com.pennet.defender.repository;

import com.pennet.defender.model.FirewallRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FirewallRuleRepository extends JpaRepository<FirewallRule, Integer> {

    Page<FirewallRule> findAllByOrderByPriorityAsc(Pageable pageable);

    List<FirewallRule> findByActionOrderByPriorityAsc(String action);

    List<FirewallRule> findByChainOrderByPriorityAsc(String chain);

    @Query("SELECT COALESCE(MAX(r.priority), 0) FROM FirewallRule r WHERE r.chain = :chain")
    int findMaxPriorityByChain(@Param("chain") String chain);
}