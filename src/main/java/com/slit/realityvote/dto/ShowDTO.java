package com.slit.realityvote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Show management operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowDTO {

    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Genre is required")
    private String genre;

    @NotBlank(message = "Banner image URL is required")
    @Pattern(
        regexp = "^https?://.*\\.(?i)(png|jpg|jpeg|webp)(\\?.*)?$",
        message = "Banner image URL must be a valid PNG, JPG, or WEBP URL"
    )
    private String bannerImageUrl;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(DRAFT|ACTIVE|ARCHIVED)$", message = "Status must be DRAFT, ACTIVE, or ARCHIVED")
    private String status;
}
