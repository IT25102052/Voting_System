package com.slit.realityvote.controller;

import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.entity.Judge;
import com.slit.realityvote.entity.RealityShow;
import com.slit.realityvote.repository.EpisodeRepository;
import com.slit.realityvote.repository.SeasonRepository;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.JudgeAssignmentService;
import com.slit.realityvote.service.JudgeService;
import com.slit.realityvote.service.RealityShowService;
import com.slit.realityvote.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JudgeController.class)
@Import(SecurityConfig.class)
class JudgeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean JudgeService judgeService;
    @MockBean JudgeAssignmentService assignmentService;
    @MockBean RealityShowService showService;
    @MockBean SeasonRepository seasonRepository;
    @MockBean EpisodeRepository episodeRepository;
    @MockBean UserService userService;
    @MockBean UserRepository userRepository;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void listJudges_returnsList() throws Exception {
        when(judgeService.search(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/judges"))
                .andExpect(status().isOk())
                .andExpect(view().name("judges/list"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void viewJudge_returnsView() throws Exception {
        Judge judge = Judge.builder().id(1L).fullName("Simon").email("simon@test.lk").build();
        when(judgeService.getById(1L)).thenReturn(judge);
        when(assignmentService.getAssignmentsForJudge(1L)).thenReturn(List.of());
        when(showService.getAllActiveShows()).thenReturn(List.of());
        when(userRepository.findByEmail("simon@test.lk")).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/admin/judges/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("judges/view"))
                .andExpect(model().attributeExists("judge"))
                .andExpect(model().attributeExists("assignments"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void panelsOverview_returnsPanels() throws Exception {
        RealityShow show = RealityShow.builder().id(10L).name("Star Show").build();
        when(showService.getAllActiveShows()).thenReturn(List.of(show));
        when(judgeService.getAllActiveJudges()).thenReturn(List.of());
        when(showService.getShowById(10L)).thenReturn(show);
        when(assignmentService.getAssignmentsForShow(10L)).thenReturn(List.of());

        mockMvc.perform(get("/admin/judges/panels").param("showId", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("judges/panels"))
                .andExpect(model().attributeExists("selectedShow"))
                .andExpect(model().attributeExists("allJudges"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void adminJudges_forbiddenForViewer() throws Exception {
        mockMvc.perform(get("/admin/judges"))
                .andExpect(status().isForbidden());
    }
}
