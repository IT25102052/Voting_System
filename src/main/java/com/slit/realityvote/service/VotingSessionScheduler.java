package com.slit.realityvote.service;

import com.slit.realityvote.entity.VotingSessionStatus;
import com.slit.realityvote.repository.VotingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class VotingSessionScheduler {

    private final VotingSessionRepository sessionRepository;
    private final VotingSessionService sessionService;

    @Scheduled(fixedRate = 60000)
    public void closeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.findByStatusOrderByStartTimeAsc(VotingSessionStatus.OPEN).stream()
                .filter(s -> !s.getEndTime().isAfter(now))
                .forEach(s -> sessionService.closeSession(s.getId()));
    }
}