package com.slit.realityvote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.dto.*;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.User;
import com.slit.realityvote.entity.UserStatus;
import com.slit.realityvote.entity.VotingSessionStatus;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.ComplianceMonitoringService;
import com.slit.realityvote.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComplianceMonitoringController.class)
@Import(SecurityConfig.class)
class ComplianceMonitoringControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ComplianceMonitoringService monitoringService;
    @MockBean UserService userService;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void monitoringDashboard_complianceOfficer_returns200() throws Exception {
        MonitoringSessionInfo info = new MonitoringSessionInfo(
                1L, "Show S1E1", VotingSessionStatus.OPEN,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2),
                "Show", 1, "Ep 1", 1, List.of()
        );
        when(monitoringService.getAllSessions()).thenReturn(List.of(info));
        when(monitoringService.getSessionInfo(1L)).thenReturn(info);

        mockMvc.perform(get("/compliance/monitoring").param("sessionId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("compliance/monitoring"))
                .andExpect(model().attributeExists("sessions", "selectedSession"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void monitoringDashboard_viewer_forbidden403() throws Exception {
        mockMvc.perform(get("/compliance/monitoring"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void getVoteTrend_returnsJson() throws Exception {
        VoteTrendPoint point = new VoteTrendPoint(0, "20:00", 10L, "Contestant A", "#00e5ff", 5L);
        when(monitoringService.getVoteTrend(eq(1L), anyInt())).thenReturn(List.of(point));

        mockMvc.perform(get("/compliance/monitoring/1/vote-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contestantName").value("Contestant A"))
                .andExpect(jsonPath("$[0].voteCount").value(5));
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void getUserTrend_returnsJson() throws Exception {
        UserCountPoint point = new UserCountPoint(0, "20:00", 12L);
        when(monitoringService.getUserCountTrend(eq(1L), anyInt())).thenReturn(List.of(point));

        mockMvc.perform(get("/compliance/monitoring/1/user-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userCount").value(12));
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void flagUser_success_returnsJson() throws Exception {
        User user = User.builder().id(5L).email("badactor@test.com").status(UserStatus.FLAGGED).build();
        when(userService.flagUser(eq(5L), anyString(), any(), anyString())).thenReturn(user);

        FlagUserRequest req = new FlagUserRequest("Suspicious pattern", "Manual review");

        mockMvc.perform(post("/compliance/monitoring/user/5/flag")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.newStatus").value("FLAGGED"));
    }
}
