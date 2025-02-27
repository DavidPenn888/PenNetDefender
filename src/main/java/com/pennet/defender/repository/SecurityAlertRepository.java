package com.pennet.defender.repository;

import com.pennet.defender.model.SecurityAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Integer> {
    Page<SecurityAlert> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<SecurityAlert> findByDetectWayOrderByTimestampDesc(int detectWay, Pageable pageable);
    Page<SecurityAlert> findByAlertTypeOrderByTimestampDesc(String alertType, Pageable pageable);
    Page<SecurityAlert> findByDetectWayAndAlertTypeOrderByTimestampDesc(int detectWay, String alertType, Pageable pageable);
}
