package com.slit.realityvote.dto.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Expertise is required")
    private String expertise;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @NotBlank(message = "Photo URL is required")
    @Pattern(
        regexp = "^https?://.*\\.(?i)(png|jpg|jpeg|webp)(\\?.*)?$",
        message = "Photo URL must be a valid PNG, JPG, or WEBP URL"
    )
    private String photoUrl;
}
