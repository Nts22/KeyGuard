package com.passmanager.service.impl;

import com.passmanager.model.entity.AuditLog;
import com.passmanager.model.entity.AuditLog.ActionType;
import com.passmanager.model.entity.AuditLog.ResultType;
import com.passmanager.model.entity.User;
import com.passmanager.repository.AuditLogRepository;
import com.passmanager.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void log(User user, ActionType action, String description, ResultType result) {
        log(user, action, description, null, null, result);
    }

    @Override
    @Transactional
    public void log(User user, ActionType action, String description, Long passwordEntryId, ResultType result) {
        log(user, action, description, passwordEntryId, null, result);
    }

    @Override
    @Transactional
    public void log(User user, ActionType action, String description, String ipAddress, ResultType result) {
        log(user, action, description, null, ipAddress, result);
    }

    @Transactional
    private void log(User user, ActionType action, String description, Long passwordEntryId, String ipAddress, ResultType result) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .description(description)
                .passwordEntryId(passwordEntryId)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .result(result)
                .build();

        auditLogRepository.save(auditLog);

        log.debug("Audit log: user={}, action={}, result={}",
                user.getUsername(), action, result);
    }

    @Override
    public List<AuditLog> findByUser(User user) {
        // Limitar a los últimos 50 registros
        Pageable pageable = PageRequest.of(0, 50);
        return auditLogRepository.findByUserOrderByTimestampDesc(user, pageable);
    }

    @Override
    public List<AuditLog> findByUserAndAction(User user, ActionType action) {
        return auditLogRepository.findByUserAndActionOrderByTimestampDesc(user, action);
    }

    @Override
    public List<AuditLog> findByUserAndDateRange(User user, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUserAndTimestampBetweenOrderByTimestampDesc(user, start, end);
    }

    @Override
    public long countRecentActivity(User user, LocalDateTime since) {
        return auditLogRepository.countByUserAndTimestampAfter(user, since);
    }
}
