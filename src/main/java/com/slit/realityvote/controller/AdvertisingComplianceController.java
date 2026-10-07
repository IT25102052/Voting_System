package com.slit.realityvote.controller;

import com.slit.realityvote.entity.Advertisement;
import com.slit.realityvote.entity.AdvertisementStatus;
import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.service.AdvertisementService;
import com.slit.realityvote.service.VotingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for Compliance Officers to review, approve, reject, schedule,
 * and oversee advertising across all sessions.
 */
@Controller
@RequestMapping("/compliance/advertising")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'COMPLIANCE_OFFICER')")
@RequiredArgsConstructor
public class AdvertisingComplianceController {

    private final AdvertisementService adService;
    private final VotingSessionService votingSessionService;

    @GetMapping
    public String dashboard(@RequestParam(required = false, defaultValue = "PENDING_REVIEW") String tab, Model model) {
        long pending  = adService.countByStatus(AdvertisementStatus.PENDING_REVIEW);
        long approved = adService.countByStatus(AdvertisementStatus.APPROVED);
        long rejected = adService.countByStatus(AdvertisementStatus.REJECTED);
        long active   = adService.countByStatus(AdvertisementStatus.ACTIVE);

        List<Advertisement> advertisements;
        if ("ALL".equalsIgnoreCase(tab)) {
            advertisements = adService.getAll();
        } else if ("APPROVED".equalsIgnoreCase(tab)) {
            advertisements = adService.getByStatus(AdvertisementStatus.APPROVED);
        } else if ("ACTIVE".equalsIgnoreCase(tab)) {
            advertisements = adService.getByStatus(AdvertisementStatus.ACTIVE);
        } else if ("REJECTED".equalsIgnoreCase(tab)) {
            advertisements = adService.getByStatus(AdvertisementStatus.REJECTED);
        } else {
            tab = "PENDING_REVIEW";
            advertisements = adService.getByStatus(AdvertisementStatus.PENDING_REVIEW);
        }

        model.addAttribute("currentTab", tab);
        model.addAttribute("pending",  pending);
        model.addAttribute("approved", approved);
        model.addAttribute("rejected", rejected);
        model.addAttribute("active",   active);
        model.addAttribute("advertisements", advertisements);
        return "compliance/advertising/dashboard";
    }

    @GetMapping("/{id}")
    public String reviewPage(@PathVariable Long id, Model model, Authentication auth) {
        Advertisement ad = adService.getById(id);
        model.addAttribute("advertisement", ad);
        model.addAttribute("campaign", ad.getCampaign());
        model.addAttribute("allSessions", votingSessionService.getAllSessions());
        model.addAttribute("assignedIds", ad.getSessions().stream().map(VotingSession::getId).toList());

        boolean isSelfReview = ad.getCreatedBy().getEmail().equalsIgnoreCase(auth.getName());
        model.addAttribute("isSelfReview", isSelfReview);
        return "compliance/advertising/review";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                           @RequestParam(required = false) String comment,
                           @RequestParam(value = "sessionIds", required = false) List<Long> sessionIds,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime displayStartTime,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime displayEndTime,
                           Authentication auth,
                           RedirectAttributes redirect) {
        try {
            adService.approveWithSchedule(id, comment, sessionIds, displayStartTime, displayEndTime, auth.getName());
            redirect.addFlashAttribute("success", "Advertisement approved and authorized for broadcast!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/compliance/advertising";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                          @RequestParam String comment,
                          Authentication auth,
                          RedirectAttributes redirect) {
        try {
            adService.reject(id, comment, auth.getName());
            redirect.addFlashAttribute("success", "Advertisement rejected.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/compliance/advertising";
    }

    // ── Session + display window scheduling ───────────────────────────────────

    @GetMapping("/{id}/schedule")
    public String schedulePage(@PathVariable Long id, Model model) {
        Advertisement ad = adService.getById(id);
        model.addAttribute("advertisement", ad);
        model.addAttribute("campaign", ad.getCampaign());
        model.addAttribute("allSessions", votingSessionService.getAllSessions());
        model.addAttribute("assignedIds", ad.getSessions().stream().map(VotingSession::getId).toList());
        return "compliance/advertising/schedule";
    }

    @PostMapping("/{id}/schedule")
    public String saveSchedule(@PathVariable Long id,
                                @RequestParam(value = "sessionIds", required = false) List<Long> sessionIds,
                                @RequestParam(required = false)
                                @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime displayStartTime,
                                @RequestParam(required = false)
                                @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime displayEndTime,
                                Authentication auth,
                                RedirectAttributes redirect) {
        try {
            adService.assignToSessions(id,
                    sessionIds != null ? sessionIds : List.of(),
                    displayStartTime, displayEndTime,
                    auth.getName());
            redirect.addFlashAttribute("success", "Schedule saved successfully.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/compliance/advertising/" + id;
    }
}
