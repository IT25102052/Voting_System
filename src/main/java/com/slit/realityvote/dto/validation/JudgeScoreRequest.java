package com.slit.realityvote.dto.validation;

import jakarta.validation.constraints.Max;
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
public class JudgeScoreRequest {

    @NotNull(message = "Episode ID is required")
    private UUID episodeId;

    @NotNull(message = "Contestant ID is required")
    private UUID contestantId;

    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Score must be at least 1")
    @Max(value = 10, message = "Score must not exceed 10")
    private Integer score;

    @Size(max = 500, message = "Comments must not exceed 500 characters")
    private String comments;
}
