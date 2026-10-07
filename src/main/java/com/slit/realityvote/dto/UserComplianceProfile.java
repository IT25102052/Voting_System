package com.slit.realityvote.dto;

import com.slit.realityvote.entity.AuditLog;
import com.slit.realityvote.entity.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Compliance view of a single user, shown in the right-side drawer.
 */
public record UserComplianceProfile(
        Long id,
        String fullName,
        String email,
        UserStatus status,
        boolean enabled,
        LocalDateTime registeredAt,
        long totalVotes,
        long totalRejections,
        long loginSuccesses,
        long loginFailures,
        long flaggedEvents,
        List<AuditLog> recentActivity
) {}
