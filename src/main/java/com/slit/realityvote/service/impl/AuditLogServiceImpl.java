package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import com.slit.realityvote.repository.AuditLogRepository;
import com.slit.realityvote.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Fraud Detection (simple, explainable rule - good for a viva since you
 * can point at the exact threshold and reasoning, unlike a black-box ML
 * approach which nobody on the team could defend under questioning):
 *
 * If the same actor triggers WINDOW_LIMIT-or-more events of a *monitored*
 * type within WINDOW_MINUTES, the triggering event gets flagged=true and
 * a SUSPICIOUS_ACTIVITY row is written. Two patterns are monitored:
 *   - repeated VOTE_REJECTED  -> looks like someone probing vote rules
 *   - repeated LOGIN_FAILURE  -> looks like a brute-force login attempt
 * This matches the requirements doc: "monitor unusual patterns such as
 * repeated voting attempts... and repeated failed login attempts."
 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    private static final int WINDOW_MINUTES = 10;
    private static final long WINDOW_LIMIT = 3;

    private static final Set<AuditEventType> MONITORED_TYPES =
            Set.of(AuditEventType.VOTE_REJECTED, AuditEventType.LOGIN_FAILURE);

    @Override
    @Transactional
    public void record(AuditEventType eventType, String description, String actorEmail) {
        String actor = (actorEmail == null || actorEmail.isBlank()) ? "anonymous" : actorEmail;

        boolean flagged = false;
        if (MONITORED_TYPES.contains(eventType)) {
            LocalDateTime since = LocalDateTime.now().minusMinutes(WINDOW_MINUTES);
            long recentCount = auditLogRepository.countByActorEmailAndEventTypeAndCreatedDateAfter(
                    actor, eventType, since);
            // +1 accounts for the event we're about to save
            flagged = (recentCount + 1) >= WINDOW_LIMIT;
        }

        auditLogRepository.save(AuditLog.builder()
                .eventType(eventType)
                .description(description)
                .actorEmail(actor)
                .flagged(flagged)
                .build());

        if (flagged) {
            auditLogRepository.save(AuditLog.builder()
                    .eventType(AuditEventType.SUSPICIOUS_ACTIVITY)
                    .description("Threshold reached: " + WINDOW_LIMIT + "+ " + eventType +
                            " events from '" + actor + "' within " + WINDOW_MINUTES + " minutes.")
                    .actorEmail(actor)
                    .flagged(true)
                    .build());
        }
    }

    @Override
    public Page<AuditLog> search(AuditEventType eventType, boolean flaggedOnly, String keyword, Pageable pageable) {
        String cleanKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return auditLogRepository.search(eventType, flaggedOnly, cleanKeyword, pageable);
    }

    @Override
    public long countFlagged() {
        return auditLogRepository.countByFlaggedTrue();
    }
}
