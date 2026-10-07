package com.slit.realityvote.service;

import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.JudgeStatus;
import com.slit.realityvote.repository.JudgeRepository;
import com.slit.realityvote.service.impl.JudgeServiceImpl;
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
class JudgeServiceTest {

    @Mock JudgeRepository judgeRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks JudgeServiceImpl judgeService;

    @Test
    void createJudge_savesNewJudge() {
        Judge judge = Judge.builder().fullName("Simon Cowell").email("simon@judges.com").build();
        when(judgeRepository.existsByEmailIgnoreCaseAndDeletedFalse("simon@judges.com")).thenReturn(false);
        when(judgeRepository.save(any(Judge.class))).thenAnswer(inv -> inv.getArgument(0));

        Judge result = judgeService.createJudge(judge, null);

        assertThat(result.getFullName()).isEqualTo("Simon Cowell");
        verify(judgeRepository).save(judge);
    }

    @Test
    void createJudge_throwsOnDuplicateEmail() {
        Judge judge = Judge.builder().fullName("Duplicate").email("simon@judges.com").build();
        when(judgeRepository.existsByEmailIgnoreCaseAndDeletedFalse("simon@judges.com")).thenReturn(true);

        assertThatThrownBy(() -> judgeService.createJudge(judge, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void getAllActiveJudges_returnsActiveList() {
        Judge j1 = Judge.builder().id(1L).fullName("Judge A").status(JudgeStatus.ACTIVE).build();
        when(judgeRepository.findByStatusAndDeletedFalseOrderByFullNameAsc(JudgeStatus.ACTIVE)).thenReturn(List.of(j1));

        List<Judge> result = judgeService.getAllActiveJudges();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Judge A");
    }

    @Test
    void deactivateJudge_marksDeletedAndInactive() {
        Judge j = Judge.builder().id(1L).fullName("Judge A").status(JudgeStatus.ACTIVE).deleted(false).build();
        when(judgeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(j));

        judgeService.deactivateJudge(1L);

        assertThat(j.isDeleted()).isTrue();
        assertThat(j.getStatus()).isEqualTo(JudgeStatus.INACTIVE);
        verify(judgeRepository).save(j);
    }
}
