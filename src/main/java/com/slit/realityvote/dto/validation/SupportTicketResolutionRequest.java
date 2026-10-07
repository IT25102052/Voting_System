package com.slit.realityvote.dto.validation;

import jakarta.validation.constraints.NotBlank;
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
public class SupportTicketResolutionRequest {

    @NotNull(message = "Ticket ID is required")
    private UUID ticketId;

    @NotBlank(message = "Resolution notes are required")
    @Size(min = 10, max = 1000, message = "Resolution notes must be between 10 and 1000 characters")
    private String resolutionNotes;

    @NotNull(message = "Status is required")
    private TicketResolutionStatus status;
}
