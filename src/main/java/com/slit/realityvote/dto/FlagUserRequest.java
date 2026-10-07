package com.slit.realityvote.dto;

/**
 * Payload for POST /compliance/monitoring/user/{id}/flag and /unflag and /warn.
 */
public record FlagUserRequest(
        String reason,
        String notes
) {}
