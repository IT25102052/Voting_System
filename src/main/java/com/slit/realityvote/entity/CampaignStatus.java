package com.slit.realityvote.entity;

/**
 * Lifecycle status of an advertising campaign.
 * DRAFT → ACTIVE → COMPLETED, or ARCHIVED at any point.
 */
public enum CampaignStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    ARCHIVED
}
