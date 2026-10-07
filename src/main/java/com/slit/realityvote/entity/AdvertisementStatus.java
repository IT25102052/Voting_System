package com.slit.realityvote.entity;

/**
 * Lifecycle status of an advertisement within a campaign.
 * DRAFT → PENDING_REVIEW → APPROVED → ACTIVE
 *                        → REJECTED (can resubmit → PENDING_REVIEW)
 * EXPIRED is reserved for future time-based expiry logic.
 */
public enum AdvertisementStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    ACTIVE,
    EXPIRED
}
