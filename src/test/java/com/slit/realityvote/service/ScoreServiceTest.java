package com.slit.realityvote.service;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.*;
import com.slit.realityvote.service.impl.ScoreServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock ScoreRepository scoreRepository;
    @Mock JudgeRepository judgeRepository;
    @Mock JudgeAssignmentRepository assignmentRepository;
    @Mock EpisodeRepository episodeRepository;
    @Mock SeasonRepository seasonRepository;
    @Mock VotingSessionRepository votingSessionRepository;
    @Mock ContestantRepository contestantRepository;

    @InjectMocks ScoreServiceImpl scoreService;

    @Test
    void submitScore_savesScoreWhenJudgingWindowIsOpen() {
        Judge judge = Judge.builder().id(1L).email("judge@realityvote.lk").build();
        RealityShow show = RealityShow.builder().id(10L).name("Talent Show").build();
        Season season = Season.builder().id(20L).show(show).build();
        Episode ep = Episode.builder().id(100L).judgingOpen(true).season(season).build();
        Contestant c = Contestant.builder().id(500L).fullName("Singer A").build();

        JudgeAssignment assignment = JudgeAssignment.builder().judge(judge).episode(ep).build();

        when(judgeRepository.findByEmailIgnoreCaseAndDeletedFalse("judge@realityvote.lk")).thenReturn(Optional.of(judge));
        when(assignmentRepository.findByJudge_IdOrderByAssignedDateDesc(1L)).thenReturn(List.of(assignment));
        when(episodeRepository.findById(100L)).thenReturn(Optional.of(ep));
        when(votingSessionRepository.findByEpisodeIdOrderByStartTimeDesc(100L)).thenReturn(List.of());
        when(contestantRepository.search(isNull(), eq(10L), isNull(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(c)));
        when(scoreRepository.findByJudge_IdAndContestant_IdAndEpisode_Id(1L, 500L, 100L)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));

        Score saved = scoreService.submitScore("judge@realityvote.lk", 500L, 100L, 88, "Great vocals!");

        assertThat(saved.getScoreValue()).isEqualTo(88);
        assertThat(saved.getRemarks()).isEqualTo("Great vocals!");
        verify(scoreRepository).save(any(Score.class));
    }

    @Test
    void submitScore_failsWhenJudgingWindowIsClosed() {
        Judge judge = Judge.builder().id(1L).email("judge@realityvote.lk").build();
        RealityShow show = RealityShow.builder().id(10L).name("Talent Show").build();
        Season season = Season.builder().id(20L).show(show).build();
        Episode ep = Episode.builder().id(100L).judgingOpen(false).season(season).build();
        Contestant c = Contestant.builder().id(500L).fullName("Singer A").build();

        JudgeAssignment assignment = JudgeAssignment.builder().judge(judge).episode(ep).build();

        when(judgeRepository.findByEmailIgnoreCaseAndDeletedFalse("judge@realityvote.lk")).thenReturn(Optional.of(judge));
        when(assignmentRepository.findByJudge_IdOrderByAssignedDateDesc(1L)).thenReturn(List.of(assignment));
        when(episodeRepository.findById(100L)).thenReturn(Optional.of(ep));
        when(votingSessionRepository.findByEpisodeIdOrderByStartTimeDesc(100L)).thenReturn(List.of());
        when(contestantRepository.search(isNull(), eq(10L), isNull(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(c)));

        assertThatThrownBy(() -> scoreService.submitScore("judge@realityvote.lk", 500L, 100L, 95, "Amazing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("judging window");
    }
}
