package com.slit.realityvote.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * An append-only audit trail row. Deliberately has no update/delete path
 * anywhere in the codebase - "the system shall ensure that votes cannot
 * be altered after submission" extends here too: once an event is
 * logged, it stays logged, satisfying "Vote Integrity" / "Auditability"
 * (retain for 5 years, per the requirements doc).
 *
 * `flagged` marks rows raised by the simple fraud-detection rule in
 * AuditLogServiceImpl (repeated rejected votes / failed logins in a
 * short window) so the Compliance Officer's dashboard can surface them
 * without scanning the whole table.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private AuditEventType eventType;

    @Column(nullable = false, length = 500)
    private String description;

    /** Email of the user who triggered the event, or "anonymous" for pre-login attempts. */
    @Column(nullable = false)
    private String actorEmail;

    @Builder.Default
    private boolean flagged = false;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
    }
}
