package com.slit.realityvote.dto;

import com.slit.realityvote.entity.UserStatus;

/**
 * Generic JSON response for compliance action endpoints (flag, warn, message, etc.).
 */
public record ComplianceActionResult(
        boolean success,
        String message,
        UserStatus newStatus
) {}
