package com.slit.realityvote.service;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    /** Records an event and runs the suspicious-pattern check for that actor. */
    void record(AuditEventType eventType, String description, String actorEmail);

    Page<AuditLog> search(AuditEventType eventType, boolean flaggedOnly, String keyword, Pageable pageable);

    long countFlagged();
}
