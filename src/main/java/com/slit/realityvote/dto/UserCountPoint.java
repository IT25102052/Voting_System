package com.slit.realityvote.dto;

/**
 * One data point in the unique-participating-user time-series chart.
 */
public record UserCountPoint(
        int bucket,
        String timeLabel,
        long userCount
) {}
