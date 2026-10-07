package com.slit.realityvote.service;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.AdvertisementRepository;
import com.slit.realityvote.repository.AdvertisingCampaignRepository;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.impl.AdvertisementServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvertisementServiceTest {

    @Mock AdvertisementRepository adRepository;
    @Mock AdvertisingCampaignRepository campaignRepository;
    @Mock UserRepository userRepository;
    @Mock VotingSessionRepository votingSessionRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks AdvertisementServiceImpl adService;

    private User marketingOfficer() {
        return User.builder().id(1L).fullName("Marketing Officer").email("marketing@realityvote.lk").build();
    }

    private User complianceOfficer() {
        return User.builder().id(2L).fullName("Compliance Officer").email("compliance@realityvote.lk").build();
    }

    private AdvertisingCampaign campaign() {
        return AdvertisingCampaign.builder().id(10L).name("Test Campaign").status(CampaignStatus.ACTIVE)
                .createdBy(marketingOfficer()).build();
    }

    private Advertisement draftAd() {
        return Advertisement.builder().id(100L).title("Vote Now!").content("Cast your vote")
                .status(AdvertisementStatus.DRAFT).campaign(campaign()).createdBy(marketingOfficer())
                .sessions(new ArrayList<>()).build();
    }

    private Advertisement pendingAd() {
        Advertisement ad = draftAd();
        ad.setStatus(AdvertisementStatus.PENDING_REVIEW);
        return ad;
    }

    @Test
    void create_setsStatusDraft_andAudits() {
        AdvertisingCampaign c = campaign();
        User officer = marketingOfficer();
        Advertisement ad = Advertisement.builder().title("New Ad").content("Content").build();

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(c));
        when(userRepository.findByEmail("marketing@realityvote.lk")).thenReturn(Optional.of(officer));
        when(adRepository.save(any())).thenAnswer(inv -> {
            Advertisement saved = inv.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        Advertisement result = adService.create(ad, 10L, "marketing@realityvote.lk");

        assertThat(result.getStatus()).isEqualTo(AdvertisementStatus.DRAFT);
        assertThat(result.getCampaign()).isEqualTo(c);
        assertThat(result.getCreatedBy()).isEqualTo(officer);
        verify(auditLogService).record(eq(AuditEventType.ADVERTISEMENT_CREATED), anyString(), eq("marketing@realityvote.lk"));
    }

    @Test
    void createWithSessions_assignsSessionsImmediately() {
        AdvertisingCampaign c = campaign();
        User officer = marketingOfficer();
        Advertisement ad = Advertisement.builder().title("Targeted Ad").content("Content").build();
        VotingSession s1 = VotingSession.builder().id(55L).build();

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(c));
        when(userRepository.findByEmail("marketing@realityvote.lk")).thenReturn(Optional.of(officer));
        when(votingSessionRepository.findAllById(List.of(55L))).thenReturn(List.of(s1));
        when(adRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Advertisement result = adService.createWithSessions(ad, 10L, List.of(55L), "marketing@realityvote.lk");

        assertThat(result.getSessions()).contains(s1);
        verify(adRepository).save(ad);
    }

    @Test
    void create_failsOnCompletedCampaign() {
        AdvertisingCampaign c = campaign();
        c.setStatus(CampaignStatus.COMPLETED);
        when(campaignRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> adService.create(new Advertisement(), 10L, "marketing@realityvote.lk"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void submitForReview_movesToPendingReview() {
        Advertisement ad = draftAd();
        when(adRepository.findById(100L)).thenReturn(Optional.of(ad));
        when(adRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Advertisement result = adService.submitForReview(100L, "marketing@realityvote.lk");

        assertThat(result.getStatus()).isEqualTo(AdvertisementStatus.PENDING_REVIEW);
        verify(auditLogService).record(eq(AuditEventType.ADVERTISEMENT_SUBMITTED), anyString(), anyString());
    }

    @Test
    void approve_setsStatusApproved_recordsReviewer() {
        Advertisement ad = pendingAd();
        User reviewer = complianceOfficer();
        when(adRepository.findById(100L)).thenReturn(Optional.of(ad));
        when(userRepository.findByEmail("compliance@realityvote.lk")).thenReturn(Optional.of(reviewer));
        when(adRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Advertisement result = adService.approve(100L, "Looks good", "compliance@realityvote.lk");

        assertThat(result.getStatus()).isEqualTo(AdvertisementStatus.APPROVED);
        assertThat(result.getReviewedBy()).isEqualTo(reviewer);
        assertThat(result.getComplianceComment()).isEqualTo("Looks good");
        assertThat(result.getReviewedAt()).isNotNull();
        verify(auditLogService).record(eq(AuditEventType.ADVERTISEMENT_APPROVED), anyString(), eq("compliance@realityvote.lk"));
    }

    @Test
    void approveWithSessions_approvesAndUpdatesTargetSessions() {
        Advertisement ad = pendingAd();
        User reviewer = complianceOfficer();
        VotingSession s1 = VotingSession.builder().id(77L).build();

        when(adRepository.findById(100L)).thenReturn(Optional.of(ad));
        when(userRepository.findByEmail("compliance@realityvote.lk")).thenReturn(Optional.of(reviewer));
        when(votingSessionRepository.findAllById(List.of(77L))).thenReturn(List.of(s1));
        when(adRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Advertisement result = adService.approveWithSessions(100L, "Approved for Session 77", List.of(77L), "compliance@realityvote.lk");

        assertThat(result.getStatus()).isEqualTo(AdvertisementStatus.APPROVED);
        assertThat(result.getSessions()).contains(s1);
    }

    @Test
    void approve_blockedForSelfReview() {
        Advertisement ad = pendingAd();
        when(adRepository.findById(100L)).thenReturn(Optional.of(ad));

        // Creator tries to approve their own ad
        assertThatThrownBy(() -> adService.approve(100L, "ok", "marketing@realityvote.lk"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot approve your own");
    }

    @Test
    void reject_requiresReason() {
        Advertisement ad = pendingAd();
        when(adRepository.findById(100L)).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> adService.reject(100L, "", "compliance@realityvote.lk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    void reject_blockedForSelfReview() {
        Advertisement ad = pendingAd();
        when(adRepository.findById(100L)).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> adService.reject(100L, "bad", "marketing@realityvote.lk"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot reject your own");
    }

    @Test
    void activate_movesApprovedToActive() {
        Advertisement ad = draftAd();
        ad.setStatus(AdvertisementStatus.APPROVED);
        when(adRepository.findById(100L)).thenReturn(Optional.of(ad));
        when(adRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Advertisement result = adService.activate(100L, "marketing@realityvote.lk");

        assertThat(result.getStatus()).isEqualTo(AdvertisementStatus.ACTIVE);
        verify(auditLogService).record(eq(AuditEventType.ADVERTISEMENT_ACTIVATED), anyString(), anyString());
    }

    @Test
    void update_onlyAllowedForDraftOrRejected() {
        Advertisement ad = pendingAd();
        when(adRepository.findById(100L)).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> adService.update(100L, new Advertisement()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT or REJECTED");
    }

    @Test
    void getActiveAdsForSession_returnsApprovedOrActiveAds() {
        Advertisement ad1 = Advertisement.builder().id(1L).status(AdvertisementStatus.APPROVED).build();
        when(adRepository.findActiveOrApprovedAdsBySessionWithinWindow(eq(10L), any())).thenReturn(List.of(ad1));

        List<Advertisement> results = adService.getActiveAdsForSession(10L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(AdvertisementStatus.APPROVED);
    }

    @Test
    void delete_onlyAllowedForDraft() {
        Advertisement draft = draftAd();
        when(adRepository.findById(100L)).thenReturn(Optional.of(draft));

        adService.delete(100L, "marketing@realityvote.lk");

        verify(adRepository).delete(draft);
    }
}
