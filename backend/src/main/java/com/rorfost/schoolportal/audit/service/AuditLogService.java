package com.rorfost.schoolportal.audit.service;

import com.rorfost.schoolportal.audit.api.AuditLogResponse;
import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.domain.AuditLog;
import com.rorfost.schoolportal.audit.repository.AuditLogRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final String EMPTY_METADATA = "{}";

    /**
     * Maximum entries returned for a single school. Protects Render Free heap from full-table scans
     * on busy tenants while remaining well above any realistic admin dashboard viewport.
     */
    private static final int MAX_LIST_SIZE = 200;

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID schoolId,
            UUID actorAdminUserId,
            AuditAction action,
            String targetType,
            UUID targetId,
            String requestId) {
        auditLogRepository.save(
                new AuditLog(
                        schoolId,
                        actorAdminUserId,
                        action,
                        targetType,
                        targetId,
                        requestId,
                        EMPTY_METADATA));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> list(UUID schoolId) {
        // Use the indexed (school_id, created_at) predicate and cap the result to avoid
        // unbounded memory consumption on tenants with large audit tables.
        return auditLogRepository
                .findBySchoolId(
                        schoolId,
                        PageRequest.of(0, MAX_LIST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
