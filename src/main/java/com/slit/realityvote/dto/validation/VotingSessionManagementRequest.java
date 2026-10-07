package com.slit.realityvote.dto.validation;

import com.slit.realityvote.validation.ValidDateRange;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidDateRange(
    startField = "startTime",
    endField = "endTime",
    message = "End time must be strictly after start time"
)
public class VotingSessionManagementRequest {

    @NotNull(message = "Episode ID is required")
    private UUID episodeId;

    @NotBlank(message = "Session name is required")
    @Size(min = 3, max = 100, message = "Session name must be between 3 and 100 characters")
    private String sessionName;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Max votes per user is required")
    @Min(value = 1, message = "Max votes per user must be at least 1")
    @Max(value = 100, message = "Max votes per user must not exceed 100")
    private Integer maxVotesPerUser;

    @NotNull(message = "Status is required")
    private SessionLifecycleStatus status;
}
