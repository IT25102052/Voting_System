package com.slit.realityvote.service;

import com.slit.realityvote.dto.*;
import com.slit.realityvote.entity.AuditEventType;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service for the real-time compliance monitoring dashboard.
 * Read-only operations for charts and feeds; write operations
 * delegate to UserService and AuditLogService.
 */
public interface ComplianceMonitoringService {

    /**
     * All voting sessions, enriched with show/season/episode/contestant info
     * for the session selector dropdown.
     */
    List<MonitoringSessionInfo> getAllSessions();

    /**
     * Single session info for the monitoring dashboard header.
     */
    MonitoringSessionInfo getSessionInfo(Long sessionId);

    /**
     * Vote time-series for each contestant in the session.
     * Grouped into intervalMinutes buckets from session start.
     * Returns cumulative counts per bucket.
     */
    List<VoteTrendPoint> getVoteTrend(Long sessionId, int intervalMinutes);

    /**
     * Unique participating user counts per time interval.
     * "Participating" = cast a vote (most reliable signal in this system).
     */
    List<UserCountPoint> getUserCountTrend(Long sessionId, int intervalMinutes);

    /**
     * Paginated user activity feed for the monitoring table.
     * Scoped to voters who have voted in the given session.
     */
    Page<UserActivityRow> getUserActivityFeed(Long sessionId,
                                               String keyword,
                                               AuditEventType eventType,
                                               int minutesAgo,
                                               int page,
                                               int size);

    /**
     * Compliance profile for a single user, used by the right-side drawer.
     */
    UserComplianceProfile getUserProfile(String email);
}
