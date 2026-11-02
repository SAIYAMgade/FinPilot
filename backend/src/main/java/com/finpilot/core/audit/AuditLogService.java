package com.finpilot.core.audit;

import com.finpilot.core.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(String entityType, String entityId, String action, String performedBy, String beforeState, String afterState) {
        AuditLog log = AuditLog.builder()
                .id("aud-" + UUID.randomUUID().toString().substring(0, 8))
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .correlationId("corr-" + UUID.randomUUID().toString().substring(0, 8))
                .beforeState(beforeState)
                .afterState(afterState)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLog> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> pageResult = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PagedResponse.from(pageResult);
    }
}
