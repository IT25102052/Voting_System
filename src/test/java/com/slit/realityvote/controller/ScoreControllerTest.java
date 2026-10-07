package com.slit.realityvote.controller;

import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.repository.EpisodeRepository;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.ScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScoreController.class)
@Import(SecurityConfig.class)
class ScoreControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ScoreService scoreService;
    @MockBean EpisodeRepository episodeRepository;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    @Test
    @WithMockUser(username = "judge@realityvote.lk", roles = "JUDGE")
    void assignedEpisodes_returnsScoreList() throws Exception {
        when(scoreService.getAssignedEpisodes("judge@realityvote.lk")).thenReturn(List.of());

        mockMvc.perform(get("/judge/scores"))
                .andExpect(status().isOk())
                .andExpect(view().name("judge/scores/list"))
                .andExpect(model().attributeExists("episodes"));
    }

    @Test
    @WithMockUser(username = "judge@realityvote.lk", roles = "JUDGE")
    void scoreHistory_returnsHistory() throws Exception {
        when(scoreService.getScoringHistory("judge@realityvote.lk")).thenReturn(List.of());

        mockMvc.perform(get("/judge/scores/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("judge/scores/history"))
                .andExpect(model().attributeExists("scores"));
    }
}
