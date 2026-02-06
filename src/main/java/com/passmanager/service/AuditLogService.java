package com.passmanager.service;

import com.passmanager.model.entity.AuditLog;
import com.passmanager.model.entity.AuditLog.ActionType;
import com.passmanager.model.entity.AuditLog.ResultType;
import com.passmanager.model.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {

    void log(User user, ActionType action, String description, ResultType result);

    void log(User user, ActionType action, String description, Long passwordEntryId, ResultType result);

    void log(User user, ActionType action, String description, String ipAddress, ResultType result);

    List<AuditLog> findByUser(User user);

    List<AuditLog> findByUserAndAction(User user, ActionType action);

    List<AuditLog> findByUserAndDateRange(User user, LocalDateTime start, LocalDateTime end);

    long countRecentActivity(User user, LocalDateTime since);
}
