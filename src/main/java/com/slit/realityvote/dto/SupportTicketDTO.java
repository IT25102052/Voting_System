package com.slit.realityvote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for support ticket resolution and management.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketDTO {

    private Long id;

    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    @Size(max = 200, message = "Subject must not exceed 200 characters")
    private String subject;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Resolution notes are required")
    @Size(min = 10, max = 1000, message = "Resolution notes must be between 10 and 1000 characters")
    private String resolutionNotes;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(IN_PROGRESS|RESOLVED|ESCALATED|CLOSED)$", message = "Status must be IN_PROGRESS, RESOLVED, ESCALATED, or CLOSED")
    private String status;
}
