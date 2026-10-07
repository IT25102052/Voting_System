package com.slit.realityvote.service.impl;

import com.slit.realityvote.dto.*;
import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.*;
import com.slit.realityvote.service.ComplianceMonitoringService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplianceMonitoringServiceImpl implements ComplianceMonitoringService {

    private final VotingSessionRepository sessionRepository;
    private final VoteRepository voteRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Highcharts colors mapped to contestant statuses
    private static final Map<String, String> STATUS_COLORS = Map.of(
            "ACTIVE",    "#00e5ff",   // cyan
            "ELIMINATED","#ffc857",   // amber
            "WITHDRAWN", "#ffc857",
            "WINNER",    "#2af0a0"    // teal-green
    );

    // ── Session info ─────────────────────────────────────────────────────────

    @Override
    public List<MonitoringSessionInfo> getAllSessions() {
        return sessionRepository.findAllByOrderByStartTimeDesc().stream()
                .map(this::toSessionInfo)
                .collect(Collectors.toList());
    }

    @Override
    public MonitoringSessionInfo getSessionInfo(Long sessionId) {
        VotingSession session = getSession(sessionId);
        return toSessionInfo(session);
    }

    private MonitoringSessionInfo toSessionInfo(VotingSession s) {
        Episode ep = s.getEpisode();
        Season season = ep.getSeason();
        RealityShow show = season.getShow();

        List<MonitoringSessionInfo.ContestantInfo> contestants = s.getContestants().stream()
                .map(c -> new MonitoringSessionInfo.ContestantInfo(
                        c.getId(),
                        c.getFullName(),
                        c.getStatus().name(),
                        STATUS_COLORS.getOrDefault(c.getStatus().name(), "#00e5ff")))
                .collect(Collectors.toList());

        String desc = show.getName() + " — " + ep.getTitle() + " (S" + season.getSeasonNumber() + "E" + ep.getEpisodeNumber() + ")";
        return new MonitoringSessionInfo(
                s.getId(), desc, s.getStatus(),
                s.getStartTime(), s.getEndTime(),
                show.getName(), season.getSeasonNumber(),
                ep.getTitle(), ep.getEpisodeNumber(),
                contestants);
    }

    // ── Vote trend ───────────────────────────────────────────────────────────

    @Override
    public List<VoteTrendPoint> getVoteTrend(Long sessionId, int intervalMinutes) {
        VotingSession session = getSession(sessionId);
        LocalDateTime start = session.getStartTime();

        // Build contestant id → info map
        Map<Long, Contestant> contestantMap = session.getContestants().stream()
                .collect(Collectors.toMap(Contestant::getId, c -> c));

        List<VoteRepository.VoteBucket> raw = voteRepository.getVoteTrend(sessionId, start, intervalMinutes);

        // Convert buckets to cumulative counts per contestant
        // First, find max bucket
        int maxBucket = raw.stream().mapToInt(VoteRepository.VoteBucket::getBucket).max().orElse(0);

        // For each contestant accumulate cumulatively
        Map<Long, Long> running = new HashMap<>();
        List<VoteTrendPoint> points = new ArrayList<>();

        // Group by bucket first
        Map<Integer, List<VoteRepository.VoteBucket>> byBucket = raw.stream()
                .collect(Collectors.groupingBy(VoteRepository.VoteBucket::getBucket));

        for (int bucket = 0; bucket <= maxBucket; bucket++) {
            String label = start.plusMinutes((long) bucket * intervalMinutes).format(TIME_FMT);
            List<VoteRepository.VoteBucket> entries = byBucket.getOrDefault(bucket, List.of());

            // Update running totals for contestants that appear in this bucket
            for (VoteRepository.VoteBucket entry : entries) {
                running.merge(entry.getContestantId(), entry.getVoteCount(), Long::sum);
            }

            // Emit one point per contestant for every bucket (cumulative)
            for (Long cId : contestantMap.keySet()) {
                Contestant c = contestantMap.get(cId);
                long cum = running.getOrDefault(cId, 0L);
                String color = STATUS_COLORS.getOrDefault(c.getStatus().name(), "#00e5ff");
                points.add(new VoteTrendPoint(bucket, label, cId, c.getFullName(), color, cum));
            }
        }

        // If no votes yet, emit one zero point per contestant
        if (points.isEmpty()) {
            String label = start.format(TIME_FMT);
            for (Contestant c : session.getContestants()) {
                String color = STATUS_COLORS.getOrDefault(c.getStatus().name(), "#00e5ff");
                points.add(new VoteTrendPoint(0, label, c.getId(), c.getFullName(), color, 0L));
            }
        }

        return points;
    }

    // ── User count trend ─────────────────────────────────────────────────────

    @Override
    public List<UserCountPoint> getUserCountTrend(Long sessionId, int intervalMinutes) {
        VotingSession session = getSession(sessionId);
        LocalDateTime start = session.getStartTime();

        List<VoteRepository.UserBucket> raw = voteRepository.getUserCountTrend(sessionId, start, intervalMinutes);

        if (raw.isEmpty()) {
            return List.of(new UserCountPoint(0, start.format(TIME_FMT), 0L));
        }

        int maxBucket = raw.stream().mapToInt(VoteRepository.UserBucket::getBucket).max().orElse(0);
        Map<Integer, Long> byBucket = raw.stream()
                .collect(Collectors.toMap(VoteRepository.UserBucket::getBucket, VoteRepository.UserBucket::getUserCount));

        List<UserCountPoint> points = new ArrayList<>();
        for (int b = 0; b <= maxBucket; b++) {
            String label = start.plusMinutes((long) b * intervalMinutes).format(TIME_FMT);
            points.add(new UserCountPoint(b, label, byBucket.getOrDefault(b, 0L)));
        }
        return points;
    }

    // ── Activity feed ────────────────────────────────────────────────────────

    @Override
    public Page<UserActivityRow> getUserActivityFeed(Long sessionId,
                                                      String keyword,
                                                      AuditEventType eventType,
                                                      int minutesAgo,
                                                      int page,
                                                      int size) {
        VotingSession session = getSession(sessionId);

        // Collect voter emails for this session
        Set<String> voterEmails = getVoterEmailsForSession(sessionId);

        // If no voters yet, show all recent audit logs (still useful)
        LocalDateTime since = minutesAgo > 0
                ? LocalDateTime.now().minusMinutes(minutesAgo)
                : session.getStartTime();

        String cleanKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();

        Page<AuditLog> logs;
        if (voterEmails.isEmpty()) {
            // No voters yet — show empty
            logs = Page.empty();
        } else {
            Collection<String> emails = voterEmails;
            if (cleanKeyword != null) {
                emails = emails.stream()
                        .filter(e -> e.toLowerCase().contains(cleanKeyword))
                        .collect(Collectors.toSet());
            }
            if (emails.isEmpty()) {
                logs = Page.empty();
            } else {
                logs = auditLogRepository.findByActorsInSession(
                        emails, eventType, since, PageRequest.of(page, size));
            }
        }

        // Build user status map
        Map<String, UserStatus> statusByEmail = new HashMap<>();
        for (String email : voterEmails) {
            userRepository.findByEmail(email).ifPresent(u -> statusByEmail.put(email, u.getStatus()));
        }

        List<UserActivityRow> rows = logs.getContent().stream()
                .map(log -> {
                    String email = log.getActorEmail();
                    UserStatus status = statusByEmail.getOrDefault(email, UserStatus.ACTIVE);
                    // Derive userName from the email prefix
                    String name = email.contains("@") ? email.split("@")[0] : email;
                    return new UserActivityRow(
                            log.getId(),
                            log.getCreatedDate(),
                            email,
                            name,
                            log.getEventType(),
                            log.getDescription(),
                            log.getIpAddress(),
                            status,
                            log.isFlagged());
                })
                .collect(Collectors.toList());

        return new PageImpl<>(rows, logs.getPageable(), logs.getTotalElements());
    }

    // ── User profile ─────────────────────────────────────────────────────────

    @Override
    public UserComplianceProfile getUserProfile(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));

        long totalVotes = voteRepository.countByVoter_Id(user.getId());
        long totalRejections = auditLogRepository.countByActorEmailAndEventType(email, AuditEventType.VOTE_REJECTED);
        long loginSuccesses = auditLogRepository.countByActorEmailAndEventType(email, AuditEventType.LOGIN_SUCCESS);
        long loginFailures = auditLogRepository.countByActorEmailAndEventType(email, AuditEventType.LOGIN_FAILURE);
        long flaggedEvents = auditLogRepository.countByActorEmailAndFlaggedTrue(email);

        List<AuditLog> recent = auditLogRepository.findTop20ByActorEmailOrderByCreatedDateDesc(email);

        return new UserComplianceProfile(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getStatus(), user.isEnabled(), user.getCreatedDate(),
                totalVotes, totalRejections, loginSuccesses, loginFailures,
                flaggedEvents, recent);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VotingSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Voting session not found: " + id));
    }

    private Set<String> getVoterEmailsForSession(Long sessionId) {
        return voteRepository.findVoterEmailsBySession(sessionId);
    }
}
