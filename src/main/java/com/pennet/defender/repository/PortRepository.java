package com.pennet.defender.repository;

import com.pennet.defender.model.Port;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortRepository extends JpaRepository<Port, Integer> {
    Page<Port> findAllByOrderByProcessNameAsc(Pageable pageable);
}