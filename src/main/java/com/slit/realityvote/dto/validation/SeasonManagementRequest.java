package com.slit.realityvote.dto.validation;

import com.slit.realityvote.validation.ValidDateRange;
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
    startField = "startDate",
    endField = "endDate",
    message = "End date must be strictly after start date"
)
public class SeasonManagementRequest {

    @NotNull(message = "Show ID is required")
    private UUID showId;

    @NotNull(message = "Season number is required")
    @Min(value = 1, message = "Season number must be a positive integer greater than or equal to 1")
    private Integer seasonNumber;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotNull(message = "Status is required")
    private SeasonStatus status;
}
