package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.ContestantRepository;
import com.slit.realityvote.repository.EpisodeRepository;
import com.slit.realityvote.repository.VoteRepository;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.AuditLogService;
import com.slit.realityvote.service.VotingSessionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VotingSessionServiceImpl implements VotingSessionService {

    private final VotingSessionRepository sessionRepository;
    private final EpisodeRepository episodeRepository;
    private final ContestantRepository contestantRepository;
    private final VoteRepository voteRepository;
    private final AuditLogService auditLogService;

    // ---------- READ ----------

    @Override
    public List<VotingSession> getAllSessions() {
        return sessionRepository.findAllByOrderByStartTimeDesc();
    }

    @Override
    public List<VotingSession> getSessionsByStatus(
            VotingSessionStatus status) {

        return sessionRepository.findByStatusOrderByStartTimeAsc(status);
    }

    @Override
    public VotingSession getById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Voting session not found with id: " + id
                        )
                );
    }

    // ---------- CREATE ----------

    @Override
    @Transactional
    public VotingSession createSession(
            Long episodeId,
            List<Long> contestantIds,
            VotingSession session) {

        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Episode not found with id: " + episodeId
                        )
                );

        validateTimes(
                session.getStartTime(),
                session.getEndTime()
        );

        List<Contestant> contestants =
                loadContestants(episode, contestantIds);

        assertNoOverlap(
                episode.getId(),
                null,
                session.getStartTime(),
                session.getEndTime()
        );

        session.setEpisode(episode);
        session.setContestants(contestants);
        session.setStatus(VotingSessionStatus.SCHEDULED);

        return sessionRepository.save(session);
    }

    // ---------- UPDATE ----------

    @Override
    @Transactional
    public VotingSession updateSession(
            Long id,
            List<Long> contestantIds,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        VotingSession session = getById(id);

        if (session.getStatus() != VotingSessionStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Only SCHEDULED sessions can be edited. " +
                            "Use 'Extend' to change the end of an open session."
            );
        }

        Episode episode = session.getEpisode();

        if (episode == null) {
            throw new IllegalStateException(
                    "This session is not linked to an episode and cannot be edited. " +
                            "Delete it and create a new one."
            );
        }

        validateTimes(startTime, endTime);

        List<Contestant> contestants =
                loadContestants(episode, contestantIds);

        assertNoOverlap(
                episode.getId(),
                id,
                startTime,
                endTime
        );

        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setContestants(contestants);

        VotingSession saved =
                sessionRepository.save(session);

        auditLogService.record(
                AuditEventType.SESSION_UPDATED,
                "Voting session " + id +
                        " updated (" + startTime +
                        " to " + endTime +
                        ", " + contestants.size() +
                        " contestants)",
                currentActorEmail()
        );

        return saved;
    }

    // ---------- EXTEND ----------

    @Override
    @Transactional
    public VotingSession extendSession(
            Long id,
            LocalDateTime newEndTime) {

        VotingSession session = getById(id);

        if (session.getStatus() == VotingSessionStatus.CLOSED) {
            throw new IllegalStateException(
                    "A closed session cannot be extended."
            );
        }

        if (newEndTime == null) {
            throw new IllegalArgumentException(
                    "Please choose a new closing time."
            );
        }

        if (!newEndTime.isAfter(session.getEndTime())) {
            throw new IllegalArgumentException(
                    "The new closing time must be later than the current one (" +
                            session.getEndTime() + ")."
            );
        }

        if (session.getEpisode() != null) {
            assertNoOverlap(
                    session.getEpisode().getId(),
                    id,
                    session.getStartTime(),
                    newEndTime
            );
        }

        LocalDateTime oldEnd =
                session.getEndTime();

        session.setEndTime(newEndTime);

        VotingSession saved =
                sessionRepository.save(session);

        auditLogService.record(
                AuditEventType.SESSION_EXTENDED,
                "Voting session " + id +
                        " extended from " + oldEnd +
                        " to " + newEndTime,
                currentActorEmail()
        );

        return saved;
    }

    // ---------- OPEN ----------

    @Override
    @Transactional
    public VotingSession openSession(Long id) {

        VotingSession session = getById(id);

        if (session.getStatus() == VotingSessionStatus.CLOSED) {
            throw new IllegalStateException(
                    "A closed session cannot be re-opened. " +
                            "Create a new session instead."
            );
        }

        if (session.getStatus() == VotingSessionStatus.OPEN) {
            throw new IllegalStateException(
                    "This session is already open."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // Cannot open before the scheduled start time.
        if (session.getStartTime().isAfter(now)) {
            throw new IllegalStateException(
                    "Voting cannot be opened before the scheduled start time."
            );
        }

        // Cannot open an expired session.
        if (!session.getEndTime().isAfter(now)) {
            throw new IllegalStateException(
                    "This session's voting window has already ended. " +
                            "Edit the session and set a new closing time first."
            );
        }

        session.setStatus(VotingSessionStatus.OPEN);

        VotingSession saved =
                sessionRepository.save(session);

        auditLogService.record(
                AuditEventType.SESSION_OPENED,
                "Voting session " + id + " opened",
                currentActorEmail()
        );

        return saved;
    }

    // ---------- CLOSE ----------

    @Override
    @Transactional
    public VotingSession closeSession(Long id) {

        VotingSession session = getById(id);

        if (session.getStatus() == VotingSessionStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "A scheduled voting session cannot be closed. Open it first."
            );
        }

        if (session.getStatus() == VotingSessionStatus.CLOSED) {
            throw new IllegalStateException(
                    "This session is already closed."
            );
        }

        session.setStatus(VotingSessionStatus.CLOSED);

        VotingSession saved =
                sessionRepository.save(session);

        auditLogService.record(
                AuditEventType.SESSION_CLOSED,
                "Voting session " + id + " closed",
                currentActorEmail()
        );

        return saved;
    }

    // ---------- DELETE ----------

    @Override
    @Transactional
    public void deleteSession(Long id) {

        VotingSession session = getById(id);

        if (session.getStatus() == VotingSessionStatus.OPEN) {
            throw new IllegalStateException(
                    "Close the session before deleting it."
            );
        }

        if (voteRepository.countByVotingSession_Id(id) > 0) {
            throw new IllegalStateException(
                    "This session already has votes and cannot be deleted. " +
                            "Close it instead."
            );
        }

        try {

            sessionRepository.delete(session);
            sessionRepository.flush();

        } catch (DataIntegrityViolationException ex) {

            throw new IllegalStateException(
                    "This session is still referenced by advertisements, " +
                            "reports or alerts and cannot be deleted."
            );
        }

        auditLogService.record(
                AuditEventType.SESSION_DELETED,
                "Voting session " + id + " deleted",
                currentActorEmail()
        );
    }

    // ---------- TIME VALIDATION ----------

    private void validateTimes(
            LocalDateTime start,
            LocalDateTime end) {

        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start and end time are required."
            );
        }

        /*
         * Compare only up to the minute.
         *
         * Example:
         * Current time = 11:53:27
         * Selected start = 11:53
         *
         * Both become 11:53, so the start time is accepted.
         */
        LocalDateTime now = LocalDateTime.now()
                .withSecond(0)
                .withNano(0);

        LocalDateTime selectedStart = start
                .withSecond(0)
                .withNano(0);

        LocalDateTime selectedEnd = end
                .withSecond(0)
                .withNano(0);

        // Start time cannot be before the current minute.
        if (selectedStart.isBefore(now)) {
            throw new IllegalArgumentException(
                    "Start time cannot be in the past."
            );
        }

        // End time must be after start time.
        if (!selectedEnd.isAfter(selectedStart)) {
            throw new IllegalArgumentException(
                    "End time must be after start time."
            );
        }

        // End time must be in the future.
        if (!selectedEnd.isAfter(now)) {
            throw new IllegalArgumentException(
                    "End time must be in the future."
            );
        }
    }

    // ---------- CONTESTANTS ----------

    private List<Contestant> loadContestants(
            Episode episode,
            List<Long> contestantIds) {

        if (contestantIds == null || contestantIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one contestant for this voting session"
            );
        }

        List<Long> distinctIds =
                contestantIds.stream()
                        .distinct()
                        .toList();

        List<Contestant> contestants =
                contestantRepository.findAllById(distinctIds);

        if (contestants.size() != distinctIds.size()) {
            throw new IllegalArgumentException(
                    "One or more selected contestants could not be found"
            );
        }

        Long showId =
                episode.getSeason()
                        .getShow()
                        .getId();

        for (Contestant c : contestants) {

            if (c.isDeleted()) {
                throw new IllegalArgumentException(
                        c.getFullName() +
                                " has been removed and cannot be added to a session."
                );
            }

            if (!c.getShow().getId().equals(showId)) {
                throw new IllegalArgumentException(
                        c.getFullName() +
                                " does not belong to this episode's show."
                );
            }
        }

        return new ArrayList<>(contestants);
    }

    // ---------- OVERLAP CHECK ----------

    private void assertNoOverlap(
            Long episodeId,
            Long excludeSessionId,
            LocalDateTime start,
            LocalDateTime end) {

        boolean overlaps =
                sessionRepository
                        .findByEpisodeIdOrderByStartTimeDesc(episodeId)
                        .stream()
                        .filter(s ->
                                !s.getId().equals(excludeSessionId)
                        )
                        .filter(s ->
                                s.getStatus() != VotingSessionStatus.CLOSED
                        )
                        .anyMatch(s ->
                                start.isBefore(s.getEndTime())
                                        && end.isAfter(s.getStartTime())
                        );

        if (overlaps) {
            throw new IllegalArgumentException(
                    "Another scheduled or open voting session for this " +
                            "episode overlaps this time window."
            );
        }
    }

    // ---------- CURRENT ADMIN ----------

    private String currentActorEmail() {

        var auth =
                org.springframework.security.core.context
                        .SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        return auth != null
                ? auth.getName()
                : "system";
    }
}