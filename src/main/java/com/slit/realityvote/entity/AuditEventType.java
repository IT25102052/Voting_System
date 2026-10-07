package com.slit.realityvote.entity;

/**
 * What kind of thing happened. Kept as a flat enum so the Compliance Officer
 * can filter the audit log reliably.
 */
public enum AuditEventType {
    VOTE_CAST,
    VOTE_REJECTED,
    SESSION_OPENED,
    SESSION_CLOSED,
    SESSION_DELETED,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    USER_REGISTERED,
    PROFILE_UPDATED,
    PASSWORD_CHANGED,
    SUSPICIOUS_ACTIVITY,
    // ── Compliance monitoring additions ──────────────────────────────────────
    USER_FLAGGED,
    USER_UNFLAGGED,
    USER_WARNED,
    COMPLIANCE_MESSAGE_SENT,
    // ── Advertising & Marketing module ──────────────────────────────────────
    CAMPAIGN_CREATED,
    CAMPAIGN_ACTIVATED,
    CAMPAIGN_COMPLETED,
    ADVERTISEMENT_CREATED,
    ADVERTISEMENT_SUBMITTED,
    ADVERTISEMENT_APPROVED,
    ADVERTISEMENT_REJECTED,
    ADVERTISEMENT_ACTIVATED,
    ADVERTISEMENT_SCHEDULED
}
