package com.pennet.defender.repository;

import com.pennet.defender.model.ThresholdAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThresholdAlertRepository extends JpaRepository<ThresholdAlert, Integer> {
    Page<ThresholdAlert> findByAlertTypeOrderByTimestampDesc(String alertType, Pageable pageable);

    Page<ThresholdAlert> findAllByOrderByTimestampDesc(Pageable pageable);
}