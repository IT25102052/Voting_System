package com.slit.realityvote.controller;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.service.AdvertisementService;
import com.slit.realityvote.service.AdvertisingCampaignService;
import com.slit.realityvote.service.VotingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for managing advertising campaigns and advertisements.
 * Both ADMINISTRATOR and MARKETING_OFFICER can manage campaigns and ads.
 */
@Controller
@RequestMapping("/marketing")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MARKETING_OFFICER')")
@RequiredArgsConstructor
public class MarketingController {

    private final AdvertisingCampaignService campaignService;
    private final AdvertisementService adService;
    private final VotingSessionService votingSessionService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping
    public String dashboard(Model model) {
        List<AdvertisingCampaign> campaigns = campaignService.getAll();
        model.addAttribute("campaigns", campaigns);
        model.addAttribute("totalCampaigns", campaigns.size());
        model.addAttribute("activeCampaigns", campaignService.countActive());
        model.addAttribute("pendingAds", adService.countByStatus(AdvertisementStatus.PENDING_REVIEW));
        model.addAttribute("approvedAds", adService.countByStatus(AdvertisementStatus.APPROVED));
        model.addAttribute("activeAds", adService.countByStatus(AdvertisementStatus.ACTIVE));
        return "marketing/dashboard";
    }

    // ── Campaigns ─────────────────────────────────────────────────────────────

    @GetMapping("/campaigns")
    public String campaignList(Model model) {
        model.addAttribute("campaigns", campaignService.getAll());
        return "marketing/campaigns/list";
    }

    @GetMapping("/campaigns/create")
    public String createCampaignForm(Model model) {
        model.addAttribute("campaign", new AdvertisingCampaign());
        return "marketing/campaigns/create";
    }

    @PostMapping("/campaigns/create")
    public String createCampaign(@ModelAttribute AdvertisingCampaign campaign,
                                  Authentication auth,
                                  RedirectAttributes redirect) {
        try {
            if (campaign.getEndDate() != null && campaign.getStartDate() != null
                    && campaign.getEndDate().isBefore(campaign.getStartDate())) {
                redirect.addFlashAttribute("error", "End date must not be before start date.");
                return "redirect:/marketing/campaigns/create";
            }
            AdvertisingCampaign saved = campaignService.create(campaign, auth.getName());
            redirect.addFlashAttribute("success", "Campaign '" + saved.getName() + "' created successfully.");
            return "redirect:/marketing/campaigns/" + saved.getId();
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketing/campaigns/create";
        }
    }

    @GetMapping("/campaigns/{id}")
    public String campaignDetail(@PathVariable Long id, Model model) {
        AdvertisingCampaign campaign = campaignService.getById(id);
        List<Advertisement> ads = adService.getByCampaign(id);
        model.addAttribute("campaign", campaign);
        model.addAttribute("advertisements", ads);
        return "marketing/campaigns/detail";
    }

    @PostMapping("/campaigns/{id}/edit")
    public String editCampaign(@PathVariable Long id,
                                @ModelAttribute AdvertisingCampaign updated,
                                RedirectAttributes redirect) {
        try {
            if (updated.getEndDate() != null && updated.getStartDate() != null
                    && updated.getEndDate().isBefore(updated.getStartDate())) {
                redirect.addFlashAttribute("error", "End date must not be before start date.");
                return "redirect:/marketing/campaigns/" + id;
            }
            campaignService.update(id, updated);
            redirect.addFlashAttribute("success", "Campaign updated successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/marketing/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/activate")
    public String activateCampaign(@PathVariable Long id,
                                    Authentication auth,
                                    RedirectAttributes redirect) {
        try {
            campaignService.activate(id, auth.getName());
            redirect.addFlashAttribute("success", "Campaign activated.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/marketing/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/complete")
    public String completeCampaign(@PathVariable Long id,
                                    Authentication auth,
                                    RedirectAttributes redirect) {
        try {
            campaignService.complete(id, auth.getName());
            redirect.addFlashAttribute("success", "Campaign marked as completed.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/marketing/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/archive")
    public String archiveCampaign(@PathVariable Long id,
                                   Authentication auth,
                                   RedirectAttributes redirect) {
        try {
            campaignService.archive(id, auth.getName());
            redirect.addFlashAttribute("success", "Campaign archived.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/marketing/campaigns/" + id;
    }

    // ── Advertisements ────────────────────────────────────────────────────────

    @GetMapping("/campaigns/{campaignId}/advertisements/create")
    public String createAdForm(@PathVariable Long campaignId, Model model) {
        model.addAttribute("campaign", campaignService.getById(campaignId));
        model.addAttribute("advertisement", new Advertisement());
        model.addAttribute("allSessions", votingSessionService.getAllSessions());
        return "marketing/advertisements/create";
    }

    @PostMapping("/campaigns/{campaignId}/advertisements/create")
    public String createAd(@PathVariable Long campaignId,
                            @ModelAttribute Advertisement ad,
                            @RequestParam(value = "sessionIds", required = false) List<Long> sessionIds,
                            Authentication auth,
                            RedirectAttributes redirect) {
        try {
            Advertisement saved = adService.createWithSessions(ad, campaignId, sessionIds, auth.getName());
            redirect.addFlashAttribute("success", "Advertisement '" + saved.getTitle() + "' created.");
            return "redirect:/marketing/advertisements/" + saved.getId();
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketing/campaigns/" + campaignId + "/advertisements/create";
        }
    }

    @GetMapping("/advertisements/{id}")
    public String adDetail(@PathVariable Long id, Model model) {
        Advertisement ad = adService.getById(id);
        model.addAttribute("advertisement", ad);
        model.addAttribute("campaign", ad.getCampaign());
        model.addAttribute("allSessions", votingSessionService.getAllSessions());
        model.addAttribute("assignedIds", ad.getSessions().stream().map(VotingSession::getId).toList());
        return "marketing/advertisements/detail";
    }

    @PostMapping("/advertisements/{id}/edit")
    public String editAd(@PathVariable Long id,
                          @ModelAttribute Advertisement updated,
                          @RequestParam(value = "sessionIds", required = false) List<Long> sessionIds,
                          RedirectAttributes redirect) {
        try {
            adService.updateWithSessions(id, updated, sessionIds);
            redirect.addFlashAttribute("success", "Advertisement updated.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/marketing/advertisements/" + id;
    }

    @PostMapping("/advertisements/{id}/delete")
    public String deleteAd(@PathVariable Long id,
                            Authentication auth,
                            RedirectAttributes redirect) {
        try {
            Advertisement ad = adService.getById(id);
            Long campaignId = ad.getCampaign().getId();
            adService.delete(id, auth.getName());
            redirect.addFlashAttribute("success", "Advertisement deleted.");
            return "redirect:/marketing/campaigns/" + campaignId;
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketing/advertisements/" + id;
        }
    }

    @PostMapping("/advertisements/{id}/submit")
    public String submitForReview(@PathVariable Long id,
                                   Authentication auth,
                                   RedirectAttributes redirect) {
        try {
            adService.submitForReview(id, auth.getName());
            redirect.addFlashAttribute("success", "Advertisement submitted for compliance review.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/marketing/advertisements/" + id;
    }

    @PostMapping("/advertisements/{id}/activate")
    public String activateAd(@PathVariable Long id,
                              Authentication auth,
                              RedirectAttributes redirect) {
        try {
            adService.activate(id, auth.getName());
            redirect.addFlashAttribute("success", "Advertisement is now active!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/marketing/advertisements/" + id;
    }
}
