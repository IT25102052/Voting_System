package com.slit.realityvote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for public and authenticated vote submissions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteSubmissionDTO {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Contestant ID is required")
    private Long contestantId;

    @NotBlank(message = "Device fingerprint is required")
    private String deviceFingerprint;

    private String captchaToken;
}
