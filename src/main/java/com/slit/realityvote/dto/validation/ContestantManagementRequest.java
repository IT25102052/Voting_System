package com.slit.realityvote.dto.validation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class ContestantManagementRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 50, message = "Stage name must not exceed 50 characters")
    private String stageName;

    @NotNull(message = "Age is required")
    @Min(value = 16, message = "Age must be at least 16")
    @Max(value = 99, message = "Age must not exceed 99")
    private Integer age;

    @NotNull(message = "Show ID is required")
    private UUID showId;

    @NotNull(message = "Season ID is required")
    private UUID seasonId;

    @Size(max = 1000, message = "Biography must not exceed 1000 characters")
    private String biography;

    @NotBlank(message = "Image URL is required")
    @Pattern(
        regexp = "^https?://.*\\.(?i)(png|jpg|jpeg|webp)(\\?.*)?$",
        message = "Image URL must be a valid PNG, JPG, or WEBP URL"
    )
    private String imageUrl;

    @NotNull(message = "Status is required")
    private ContestantLifecycleStatus status;
}
