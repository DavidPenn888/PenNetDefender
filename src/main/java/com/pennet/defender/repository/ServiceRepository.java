package com.pennet.defender.repository;

import com.pennet.defender.model.AppService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<AppService, Integer> {
    Page<AppService> findAllByOrderByNameAsc(Pageable pageable);
    AppService findByName(String name);
    Page<AppService> findByNameContainingIgnoreCase(String name, Pageable pageable);
}