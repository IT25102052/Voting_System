package com.slit.realityvote.service;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.*;
import com.slit.realityvote.service.impl.JudgeAssignmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeAssignmentServiceTest {

    @Mock JudgeAssignmentRepository assignmentRepository;
    @Mock JudgeRepository judgeRepository;
    @Mock RealityShowRepository showRepository;
    @Mock SeasonRepository seasonRepository;
    @Mock EpisodeRepository episodeRepository;

    @InjectMocks JudgeAssignmentServiceImpl assignmentService;

    @Test
    void assignJudge_createsAssignment() {
        Judge judge = Judge.builder().id(1L).fullName("Paula Abdul").build();
        RealityShow show = RealityShow.builder().id(10L).name("Star Search").build();

        when(judgeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(judge));
        when(showRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(show));
        when(assignmentRepository.existsByJudge_IdAndShow_IdAndSeason_IdAndEpisode_Id(1L, 10L, null, null)).thenReturn(false);
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JudgeAssignment assignment = assignmentService.assignJudge(1L, 10L, null, null, "Main Jury");

        assertThat(assignment.getJudge()).isEqualTo(judge);
        assertThat(assignment.getShow()).isEqualTo(show);
        assertThat(assignment.getPanelName()).isEqualTo("Main Jury");
        verify(assignmentRepository).save(any());
    }

    @Test
    void assignJudge_preventsDuplicate() {
        Judge judge = Judge.builder().id(1L).build();
        RealityShow show = RealityShow.builder().id(10L).build();

        when(judgeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(judge));
        when(showRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(show));
        when(assignmentRepository.existsByJudge_IdAndShow_IdAndSeason_IdAndEpisode_Id(1L, 10L, null, null)).thenReturn(true);

        assertThatThrownBy(() -> assignmentService.assignJudge(1L, 10L, null, null, "Main Jury"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    void removeAssignment_deletesWhenExists() {
        when(assignmentRepository.existsById(50L)).thenReturn(true);

        assignmentService.removeAssignment(50L);

        verify(assignmentRepository).deleteById(50L);
    }
}
