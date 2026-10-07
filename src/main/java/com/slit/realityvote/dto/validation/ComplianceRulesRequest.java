package com.slit.realityvote.dto.validation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceRulesRequest {

    @NotNull(message = "Session ID is required")
    private UUID sessionId;

    @NotNull(message = "Max votes per IP is required")
    @Min(value = 1, message = "Max votes per IP must be at least 1")
    private Integer maxVotesPerIp;

    @NotNull(message = "Velocity limit per minute is required")
    @Min(value = 5, message = "Velocity limit per minute must be at least 5")
    private Integer velocityLimitPerMinute;

    @NotNull(message = "Auto flag suspicious IPs setting is required")
    private Boolean autoFlagSuspiciousIps;

    @Size(max = 250, message = "Rule notes must not exceed 250 characters")
    private String ruleNotes;
}
