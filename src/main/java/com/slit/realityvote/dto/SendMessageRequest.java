package com.slit.realityvote.dto;

/**
 * Payload for POST /compliance/monitoring/user/{id}/message.
 */
public record SendMessageRequest(
        String subject,
        String body
) {}
