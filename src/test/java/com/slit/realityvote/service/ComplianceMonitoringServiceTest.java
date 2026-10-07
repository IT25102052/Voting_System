package com.slit.realityvote.service;

import com.slit.realityvote.dto.*;
import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.*;
import com.slit.realityvote.service.impl.ComplianceMonitoringServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceMonitoringServiceTest {

    @Mock VotingSessionRepository sessionRepository;
    @Mock VoteRepository voteRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    ComplianceMonitoringServiceImpl monitoringService;

    @Test
    void getAllSessions_returnsEnrichedSessionInfo() {
        RealityShow show = RealityShow.builder().id(1L).name("Star Search").build();
        Season season = Season.builder().id(1L).seasonNumber(2).show(show).build();
        Episode episode = Episode.builder().id(1L).episodeNumber(5).title("Semifinals").season(season).build();
        Contestant c1 = Contestant.builder().id(10L).fullName("Alice").status(ContestantStatus.ACTIVE).build();

        VotingSession session = VotingSession.builder()
                .id(100L)
                .episode(episode)
                .contestants(List.of(c1))
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .status(VotingSessionStatus.OPEN)
                .build();

        when(sessionRepository.findAllByOrderByStartTimeDesc()).thenReturn(List.of(session));

        List<MonitoringSessionInfo> list = monitoringService.getAllSessions();
        assertThat(list).hasSize(1);
        MonitoringSessionInfo info = list.get(0);
        assertThat(info.id()).isEqualTo(100L);
        assertThat(info.showName()).isEqualTo("Star Search");
        assertThat(info.seasonNumber()).isEqualTo(2);
        assertThat(info.episodeTitle()).isEqualTo("Semifinals");
        assertThat(info.contestants()).hasSize(1);
        assertThat(info.contestants().get(0).name()).isEqualTo("Alice");
    }

    @Test
    void getUserProfile_returnsCompleteProfile() {
        User user = User.builder()
                .id(50L)
                .fullName("John Voter")
                .email("john@example.com")
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .createdDate(LocalDateTime.now().minusDays(10))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(voteRepository.countByVoter_Id(50L)).thenReturn(15L);
        when(auditLogRepository.countByActorEmailAndEventType("john@example.com", AuditEventType.VOTE_REJECTED)).thenReturn(2L);
        when(auditLogRepository.countByActorEmailAndEventType("john@example.com", AuditEventType.LOGIN_SUCCESS)).thenReturn(8L);
        when(auditLogRepository.countByActorEmailAndEventType("john@example.com", AuditEventType.LOGIN_FAILURE)).thenReturn(1L);
        when(auditLogRepository.countByActorEmailAndFlaggedTrue("john@example.com")).thenReturn(0L);
        when(auditLogRepository.findTop20ByActorEmailOrderByCreatedDateDesc("john@example.com")).thenReturn(List.of());

        UserComplianceProfile profile = monitoringService.getUserProfile("john@example.com");

        assertThat(profile.id()).isEqualTo(50L);
        assertThat(profile.email()).isEqualTo("john@example.com");
        assertThat(profile.totalVotes()).isEqualTo(15L);
        assertThat(profile.totalRejections()).isEqualTo(2L);
        assertThat(profile.loginSuccesses()).isEqualTo(8L);
        assertThat(profile.status()).isEqualTo(UserStatus.ACTIVE);
    }
}
