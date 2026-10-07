package com.slit.realityvote.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An advertisement within a campaign. Goes through a compliance
 * review workflow: DRAFT → PENDING_REVIEW → APPROVED/REJECTED → ACTIVE.
 * The creator cannot approve or reject their own advertisement.
 */
@Entity
@Table(name = "advertisements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Content is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Optional plain URL to an image banner or logo. */
    private String imageUrl;

    /** Optional sponsor website or promotional target URL (e.g. https://sponsor.com). */
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(30) DEFAULT 'DRAFT'")
    @Builder.Default
    private AdvertisementStatus status = AdvertisementStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdvertisingCampaign campaign;

    /** The Admin or Marketing Officer who created this advertisement. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /** The Compliance Officer who reviewed this advertisement (null until reviewed). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    /** When the compliance review happened. */
    private LocalDateTime reviewedAt;

    /** Comment from the compliance officer (reason for rejection, or optional approval note). */
    @Column(length = 2000)
    private String complianceComment;

    /** Voting sessions this advertisement is assigned to appear in. */
    @ManyToMany
    @JoinTable(
            name = "advertisement_sessions",
            joinColumns = @JoinColumn(name = "advertisement_id"),
            inverseJoinColumns = @JoinColumn(name = "voting_session_id")
    )
    @Builder.Default
    private List<VotingSession> sessions = new ArrayList<>();

    /** When the ad becomes eligible to show. null = eligible immediately. */
    private LocalDateTime displayStartTime;

    /** When the ad stops being eligible. null = no end (keeps showing while ACTIVE/APPROVED). */
    private LocalDateTime displayEndTime;

    /** The Compliance Officer who last set or confirmed the session/time schedule. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_by_id")
    private User scheduledBy;

    /** When the schedule was last saved. */
    private LocalDateTime scheduledAt;

    @Column(updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
