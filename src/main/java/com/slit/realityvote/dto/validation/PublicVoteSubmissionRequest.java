package com.slit.realityvote.dto.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVoteSubmissionRequest {

    @NotNull(message = "Session ID is required")
    private UUID sessionId;

    @NotNull(message = "Contestant ID is required")
    private UUID contestantId;

    @NotBlank(message = "Device fingerprint is required")
    private String deviceFingerprint;

    private String captchaToken;
}
