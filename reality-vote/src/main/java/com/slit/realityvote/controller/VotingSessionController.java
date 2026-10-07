package com.slit.realityvote.controller;

import com.slit.realityvote.dto.VotingSessionForm;
import com.slit.realityvote.entity.VotingSession;
import com.slit.realityvote.entity.VotingSessionStatus;
import com.slit.realityvote.service.ContestantService;
import com.slit.realityvote.service.RealityShowService;
import com.slit.realityvote.service.VoteService;
import com.slit.realityvote.service.VotingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Administrator-only: create voting sessions and control their lifecycle.
 *
 * Casting an actual vote is handled separately in VoteController,
 * which is what viewers use.
 */
@Controller
@RequestMapping("/admin/voting-sessions")
@RequiredArgsConstructor
public class VotingSessionController {

    private final VotingSessionService sessionService;
    private final RealityShowService showService;
    private final ContestantService contestantService;
    private final VoteService voteService;


    // =========================================================
    // LIST ALL VOTING SESSIONS
    // =========================================================

    @GetMapping
    public String list(Model model) {
        model.addAttribute("sessions", sessionService.getAllSessions());
        return "voting-sessions/list";
    }


    // =========================================================
    // CREATE VOTING SESSION - FORM
    // =========================================================

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("votingSession", new VotingSession());
        model.addAttribute("shows", showService.getAllActiveShows());

        return "voting-sessions/form";
    }


    // =========================================================
    // CREATE VOTING SESSION
    // =========================================================

    @PostMapping
    public String create(
            @Valid @ModelAttribute("votingSession") VotingSession session,
            BindingResult result,
            @RequestParam(required = false) Long episodeId,
            @RequestParam(required = false) List<Long> contestantIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (episodeId == null) {
            result.reject(
                    "episodeId",
                    "Please select a show and an episode."
            );
        }

        if (contestantIds == null || contestantIds.isEmpty()) {
            result.reject(
                    "contestantIds",
                    "Select at least one contestant for this voting session."
            );
        }

        if (result.hasErrors()) {
            model.addAttribute(
                    "shows",
                    showService.getAllActiveShows()
            );

            return "voting-sessions/form";
        }

        try {

            sessionService.createSession(
                    episodeId,
                    contestantIds,
                    session
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Voting session created (currently SCHEDULED)."
            );

            return "redirect:/admin/voting-sessions";

        } catch (IllegalArgumentException | IllegalStateException ex) {

            model.addAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            model.addAttribute(
                    "shows",
                    showService.getAllActiveShows()
            );

            return "voting-sessions/form";
        }
    }


    // =========================================================
    // VIEW VOTING SESSION
    // =========================================================

    @GetMapping("/{id}")
    public String view(
            @PathVariable Long id,
            Model model) {

        VotingSession session = sessionService.getById(id);

        model.addAttribute(
                "votingSession",
                session
        );

        model.addAttribute(
                "results",
                voteService.getLiveResults(id)
        );

        return "voting-sessions/view";
    }


    // =========================================================
    // OPEN VOTING SESSION
    // =========================================================

    @PostMapping("/{id}/open")
    public String open(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            sessionService.openSession(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Voting session is now OPEN."
            );

        } catch (IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/voting-sessions/" + id;
    }


    // =========================================================
    // CLOSE VOTING SESSION
    // =========================================================

    @PostMapping("/{id}/close")
    public String close(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            sessionService.closeSession(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Voting session is now CLOSED."
            );

        } catch (IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/voting-sessions/" + id;
    }


    // =========================================================
    // EDIT VOTING SESSION - FORM
    // =========================================================

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        VotingSession session = sessionService.getById(id);

        // Only scheduled sessions can be edited
        if (session.getStatus() != VotingSessionStatus.SCHEDULED) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Only SCHEDULED sessions can be edited."
            );

            return "redirect:/admin/voting-sessions/" + id;
        }

        // Episode cannot be changed
        if (session.getEpisode() == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This session is not linked to an episode. " +
                            "Delete it and create a new one."
            );

            return "redirect:/admin/voting-sessions/" + id;
        }

        VotingSessionForm form = new VotingSessionForm();

        form.setStartTime(
                session.getStartTime()
        );

        form.setEndTime(
                session.getEndTime()
        );

        model.addAttribute(
                "sessionForm",
                form
        );

        // Get currently selected contestants
        Set<Long> selected = new HashSet<>();

        session.getContestants()
                .forEach(c -> selected.add(c.getId()));

        populateEditModel(
                model,
                id,
                selected
        );

        return "voting-sessions/edit";
    }


    // =========================================================
    // UPDATE VOTING SESSION
    // =========================================================

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("sessionForm") VotingSessionForm form,
            BindingResult result,
            @RequestParam(required = false) List<Long> contestantIds,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (contestantIds == null || contestantIds.isEmpty()) {

            result.reject(
                    "contestantIds",
                    "Select at least one contestant for this voting session."
            );
        }

        if (!result.hasErrors()) {

            try {

                sessionService.updateSession(
                        id,
                        contestantIds,
                        form.getStartTime(),
                        form.getEndTime()
                );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Voting session updated."
                );

                return "redirect:/admin/voting-sessions/" + id;

            } catch (IllegalArgumentException | IllegalStateException ex) {

                model.addAttribute(
                        "errorMessage",
                        ex.getMessage()
                );
            }
        }

        Set<Long> selected = contestantIds == null
                ? new HashSet<>()
                : new HashSet<>(contestantIds);

        populateEditModel(
                model,
                id,
                selected
        );

        return "voting-sessions/edit";
    }


    // =========================================================
    // EXTEND VOTING SESSION
    // =========================================================

    @PostMapping("/{id}/extend")
    public String extend(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
            LocalDateTime newEndTime,
            RedirectAttributes redirectAttributes) {

        try {

            sessionService.extendSession(
                    id,
                    newEndTime
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Voting window extended."
            );

        } catch (IllegalArgumentException | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );
        }

        return "redirect:/admin/voting-sessions/" + id;
    }


    // =========================================================
    // DELETE VOTING SESSION
    // =========================================================

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            sessionService.deleteSession(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Voting session deleted."
            );

            return "redirect:/admin/voting-sessions";

        } catch (IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/admin/voting-sessions/" + id;
        }
    }


    // =========================================================
    // AJAX: EPISODES BY SHOW
    // =========================================================

    @GetMapping("/episodes-by-show")
    @ResponseBody
    public List<EpisodeOption> episodesByShow(
            @RequestParam Long showId) {

        return showService
                .getShowById(showId)
                .getSeasons()
                .stream()
                .flatMap(
                        season -> season.getEpisodes().stream()
                )
                .map(
                        e -> new EpisodeOption(
                                e.getId(),
                                e.getEpisodeNumber(),
                                e.getTitle()
                        )
                )
                .toList();
    }


    // =========================================================
    // AJAX: CONTESTANTS BY SHOW
    // =========================================================

    @GetMapping("/contestants-by-show")
    @ResponseBody
    public List<ContestantOption> contestantsByShow(
            @RequestParam Long showId) {

        return contestantService
                .search(
                        null,
                        showId,
                        null,
                        PageRequest.of(0, 100)
                )
                .getContent()
                .stream()
                .map(
                        c -> new ContestantOption(
                                c.getId(),
                                c.getFullName()
                        )
                )
                .toList();
    }


    // =========================================================
    // PREPARE EDIT PAGE DATA
    // =========================================================

    private void populateEditModel(
            Model model,
            Long id,
            Set<Long> selectedIds) {

        VotingSession session =
                sessionService.getById(id);

        Long showId =
                session
                        .getEpisode()
                        .getSeason()
                        .getShow()
                        .getId();

        model.addAttribute(
                "votingSession",
                session
        );

        model.addAttribute(
                "showContestants",
                contestantService
                        .search(
                                null,
                                showId,
                                null,
                                PageRequest.of(0, 100)
                        )
                        .getContent()
        );

        model.addAttribute(
                "selectedContestantIds",
                selectedIds
        );
    }


    // =========================================================
    // AJAX RESPONSE RECORDS
    // =========================================================

    public record EpisodeOption(
            Long id,
            Integer episodeNumber,
            String title
    ) {
    }

    public record ContestantOption(
            Long id,
            String fullName
    ) {
    }
}