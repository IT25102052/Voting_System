package com.slit.realityvote.controller;

import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.entity.*;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.AdvertisementService;
import com.slit.realityvote.service.AdvertisingCampaignService;
import com.slit.realityvote.service.VotingSessionService;
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

@WebMvcTest(MarketingController.class)
@Import(SecurityConfig.class)
class MarketingControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdvertisingCampaignService campaignService;
    @MockBean AdvertisementService adService;
    @MockBean VotingSessionService votingSessionService;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    @Test
    @WithMockUser(roles = "MARKETING_OFFICER")
    void dashboard_marketingOfficer_returns200() throws Exception {
        when(campaignService.getAll()).thenReturn(List.of());
        when(campaignService.countActive()).thenReturn(0L);
        when(adService.countByStatus(AdvertisementStatus.PENDING_REVIEW)).thenReturn(0L);
        when(adService.countByStatus(AdvertisementStatus.APPROVED)).thenReturn(0L);
        when(adService.countByStatus(AdvertisementStatus.ACTIVE)).thenReturn(0L);

        mockMvc.perform(get("/marketing"))
                .andExpect(status().isOk())
                .andExpect(view().name("marketing/dashboard"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void dashboard_viewer_forbidden403() throws Exception {
        mockMvc.perform(get("/marketing"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_OFFICER")
    void dashboard_complianceOfficer_forbidden403() throws Exception {
        mockMvc.perform(get("/marketing"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void dashboard_admin_returns200() throws Exception {
        when(campaignService.getAll()).thenReturn(List.of());
        when(campaignService.countActive()).thenReturn(0L);
        when(adService.countByStatus(AdvertisementStatus.PENDING_REVIEW)).thenReturn(0L);
        when(adService.countByStatus(AdvertisementStatus.APPROVED)).thenReturn(0L);
        when(adService.countByStatus(AdvertisementStatus.ACTIVE)).thenReturn(0L);

        mockMvc.perform(get("/marketing"))
                .andExpect(status().isOk());
    }
}
