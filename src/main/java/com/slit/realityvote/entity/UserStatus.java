package com.slit.realityvote.entity;

/**
 * Compliance status for a user account.
 * Progression: ACTIVE → WARNING → FLAGGED → SUSPENDED
 * (officers can also move backwards, e.g. FLAGGED → ACTIVE after review).
 */
public enum UserStatus {
    ACTIVE,
    WARNING,
    FLAGGED,
    SUSPENDED
}
