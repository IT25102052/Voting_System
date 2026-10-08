package com.slit.realityvote.service;

import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.entity.VotingSessionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface VotingSessionService {

    List<VotingSession> getAllSessions();

    List<VotingSession> getSessionsByStatus(VotingSessionStatus status);

    VotingSession getById(Long id);

    VotingSession createSession(Long episodeId, List<Long> contestantIds, VotingSession session);

    VotingSession updateSession(Long id, List<Long> contestantIds, LocalDateTime startTime, LocalDateTime endTime);

    VotingSession extendSession(Long id, LocalDateTime newEndTime);

    VotingSession openSession(Long id);

    VotingSession closeSession(Long id);

    void deleteSession(Long id);
}