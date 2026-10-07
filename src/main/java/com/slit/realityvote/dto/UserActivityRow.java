package com.slit.realityvote.dto;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.UserStatus;

import java.time.LocalDateTime;

/**
 * One row in the live user activity monitoring table.
 */
public record UserActivityRow(
        Long logId,
        LocalDateTime time,
        String userEmail,
        String userName,
        AuditEventType activityType,
        String description,
        String ipAddress,
        UserStatus userStatus,
        boolean flagged
) {}
