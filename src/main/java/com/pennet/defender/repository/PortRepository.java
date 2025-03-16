package com.pennet.defender.repository;

import com.pennet.defender.model.Port;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PortRepository extends JpaRepository<Port, Integer> {
    Page<Port> findAllByOrderByProcessNameAsc(Pageable pageable);
    
    // 按端口号查询
    Page<Port> findByPortNumber(Integer portNumber, Pageable pageable);
    
    // 按进程ID查询
    Page<Port> findByProcessId(Integer processId, Pageable pageable);
    
    // 按子进程ID查询（使用LIKE查询，因为子进程ID是以逗号分隔的字符串）
    @Query("SELECT p FROM Port p WHERE p.childProcessIds LIKE CONCAT('%', :childProcessId, '%') OR p.childProcessIds LIKE CONCAT('%', :childProcessId, ',%')")
    Page<Port> findByChildProcessIdContaining(@Param("childProcessId") String childProcessId, Pageable pageable);
}