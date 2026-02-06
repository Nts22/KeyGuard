package com.passmanager.repository;

import com.passmanager.model.entity.AuditLog;
import com.passmanager.model.entity.AuditLog.ActionType;
import com.passmanager.model.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUserOrderByTimestampDesc(User user, Pageable pageable);

    List<AuditLog> findByUserAndActionOrderByTimestampDesc(User user, ActionType action);

    List<AuditLog> findByUserAndTimestampBetweenOrderByTimestampDesc(
            User user, LocalDateTime start, LocalDateTime end);

    long countByUserAndTimestampAfter(User user, LocalDateTime since);
}
