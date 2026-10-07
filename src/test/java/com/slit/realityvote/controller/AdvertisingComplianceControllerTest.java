package com.slit.realityvote.controller;

import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.entity.*;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.AdvertisementService;
import com.slit.realityvote.service.VotingSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdvertisingComplianceController.class)
@Import(SecurityConfig.class)
class AdvertisingComplianceControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdvertisementService adService;
    @MockBean VotingSessionService votingSessionService;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void dashboard_complianceOfficer_returns200() throws Exception {
        when(adService.countByStatus(AdvertisementStatus.PENDING_REVIEW)).thenReturn(2L);
        when(adService.countByStatus(AdvertisementStatus.APPROVED)).thenReturn(5L);
        when(adService.countByStatus(AdvertisementStatus.REJECTED)).thenReturn(1L);
        when(adService.countByStatus(AdvertisementStatus.ACTIVE)).thenReturn(3L);
        when(adService.getByStatus(AdvertisementStatus.PENDING_REVIEW)).thenReturn(List.of());

        mockMvc.perform(get("/compliance/advertising"))
                .andExpect(status().isOk())
                .andExpect(view().name("compliance/advertising/dashboard"))
                .andExpect(model().attribute("pending", 2L));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void dashboard_viewer_forbidden403() throws Exception {
        mockMvc.perform(get("/compliance/advertising"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MARKETING_OFFICER")
    void dashboard_marketingOfficer_forbidden403() throws Exception {
        mockMvc.perform(get("/compliance/advertising"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "compliance@realityvote.lk", roles = "COMPLIANCE_OFFICER")
    void approve_redirectsToQueue() throws Exception {
        User creator = User.builder().id(1L).email("marketing@realityvote.lk").build();
        Advertisement ad = Advertisement.builder().id(50L).title("Ad").content("content")
                .status(AdvertisementStatus.PENDING_REVIEW).createdBy(creator).build();

        when(adService.approveWithSchedule(eq(50L), eq("LGTM"), any(), any(), any(), eq("compliance@realityvote.lk"))).thenReturn(ad);

        mockMvc.perform(post("/compliance/advertising/50/approve")
                        .with(csrf())
                        .param("comment", "LGTM"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/compliance/advertising"));
    }
}
