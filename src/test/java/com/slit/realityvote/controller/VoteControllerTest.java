package com.slit.realityvote.controller;

import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.entity.*;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.AdvertisementService;
import com.slit.realityvote.service.AuthBridgeService;
import com.slit.realityvote.service.VoteService;
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

@WebMvcTest(VoteController.class)
@Import(SecurityConfig.class)
class VoteControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean VoteService voteService;
    @MockBean VotingSessionService sessionService;
    @MockBean AuthBridgeService authBridgeService;
    @MockBean AdvertisementService advertisementService;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    @Test
    @WithMockUser(roles = "VIEWER")
    void openSessions_returnsSessionsAndFeaturedAds() throws Exception {
        RealityShow show = RealityShow.builder().id(1L).name("Super Star").build();
        Season season = Season.builder().id(1L).seasonNumber(1).show(show).build();
        Episode episode = Episode.builder().id(1L).title("Ep 1").episodeNumber(1).season(season).build();
        VotingSession session = VotingSession.builder().id(1L).episode(episode).status(VotingSessionStatus.OPEN).build();
        Advertisement ad = Advertisement.builder().id(10L).title("Featured Sponsor").content("Sponsor Copy").build();

        when(sessionService.getSessionsByStatus(VotingSessionStatus.OPEN)).thenReturn(List.of(session));
        when(advertisementService.getAdsForOpenSessions()).thenReturn(List.of(ad));

        mockMvc.perform(get("/vote"))
                .andExpect(status().isOk())
                .andExpect(view().name("vote/sessions"))
                .andExpect(model().attributeExists("sessions"))
                .andExpect(model().attributeExists("featuredAds"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void votingPage_loadsApprovedAdsForRelevantSession() throws Exception {
        RealityShow show = RealityShow.builder().id(1L).name("Super Star").build();
        Season season = Season.builder().id(1L).seasonNumber(1).show(show).build();
        Episode episode = Episode.builder().id(1L).title("Grand Finale").episodeNumber(1).season(season).build();
        VotingSession session = VotingSession.builder().id(5L).episode(episode).contestants(List.of()).build();

        Advertisement ad = Advertisement.builder().id(20L).title("Drink Spark").content("Vote with energy")
                .status(AdvertisementStatus.APPROVED).build();

        when(sessionService.getById(5L)).thenReturn(session);
        when(voteService.getLiveResults(5L)).thenReturn(List.of());
        when(advertisementService.getActiveAdsForSession(5L)).thenReturn(List.of(ad));

        mockMvc.perform(get("/vote/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("vote/cast"))
                .andExpect(model().attribute("advertisements", List.of(ad)));
    }
}
