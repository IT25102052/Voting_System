package com.slit.realityvote.dto;

/**
 * One data point in a per-contestant vote time-series.
 * bucket = minutes elapsed since session start, divided by intervalMinutes.
 */
public record VoteTrendPoint(
        int bucket,
        String timeLabel,
        Long contestantId,
        String contestantName,
        String contestantColor,
        long voteCount
) {}
