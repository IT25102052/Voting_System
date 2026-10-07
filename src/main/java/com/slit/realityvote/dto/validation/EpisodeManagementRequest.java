package com.slit.realityvote.dto.validation;

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
public class EpisodeManagementRequest {

    @NotNull(message = "Season ID is required")
    private UUID seasonId;

    @NotNull(message = "Episode number is required")
    @Min(value = 1, message = "Episode number must be a positive integer greater than or equal to 1")
    private Integer episodeNumber;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 150, message = "Title must be between 2 and 150 characters")
    private String title;

    @NotNull(message = "Air date is required")
    private LocalDateTime airDate;

    @Size(max = 1000, message = "Synopsis must not exceed 1000 characters")
    private String synopsis;
}
