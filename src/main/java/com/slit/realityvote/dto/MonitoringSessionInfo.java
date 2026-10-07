package com.slit.realityvote.dto;

import com.slit.realityvote.entity.VotingSessionStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Session summary shown in the session selector on the monitoring dashboard.
 */
public record MonitoringSessionInfo(
        Long id,
        String description,
        VotingSessionStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String showName,
        int seasonNumber,
        String episodeTitle,
        int episodeNumber,
        List<ContestantInfo> contestants
) {
    public record ContestantInfo(Long id, String name, String status, String color) {}
}
