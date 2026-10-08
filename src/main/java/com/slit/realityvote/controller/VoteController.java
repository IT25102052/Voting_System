package com.slit.realityvote.controller;

import com.slit.realityvote.entity.User;
import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.entity.VotingSessionStatus;
import com.slit.realityvote.service.AdvertisementService;
import com.slit.realityvote.service.AuthBridgeService;
import com.slit.realityvote.service.VoteService;
import com.slit.realityvote.service.VotingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Viewer-facing voting flow: see open sessions, view live results, cast vote.
 * Displays approved/active sponsored advertisements for the relevant voting session.
 */
@Controller
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final VotingSessionService sessionService;
    private final AuthBridgeService authBridgeService;
    private final AdvertisementService advertisementService;

    @GetMapping
    public String openSessions(Model model) {
        List<VotingSession> openSessions = sessionService.getSessionsByStatus(VotingSessionStatus.OPEN);
        model.addAttribute("sessions", openSessions);
        model.addAttribute("featuredAds", advertisementService.getAdsForOpenSessions());
        return "vote/sessions";
    }

    @GetMapping("/{sessionId}")
    public String votingPage(@PathVariable Long sessionId, Model model) {
        model.addAttribute("votingSession", sessionService.getById(sessionId));
        model.addAttribute("results", voteService.getLiveResults(sessionId));
        model.addAttribute("advertisements", advertisementService.getActiveAdsForSession(sessionId));
        return "vote/cast";
    }

    @PostMapping("/{sessionId}/contestants/{contestantId}")
    public String cast(@PathVariable Long sessionId, @PathVariable Long contestantId,
                       Authentication authentication, RedirectAttributes redirectAttributes) {
        User voter = authBridgeService.resolveCurrentUser(authentication);
        try {
            voteService.castVote(sessionId, contestantId, voter.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Your vote has been successfully recorded.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "You have already voted for this contestant.");
        }
        return "redirect:/vote/" + sessionId;
    }

    @GetMapping("/history")
    public String history(Authentication authentication, Model model) {
        User voter = authBridgeService.resolveCurrentUser(authentication);
        model.addAttribute("votes", voteService.getVotingHistoryForVoter(voter.getId()));
        return "vote/history";
    }
}
