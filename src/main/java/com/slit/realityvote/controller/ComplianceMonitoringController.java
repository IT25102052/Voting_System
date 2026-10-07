package com.slit.realityvote.controller;

import com.slit.realityvote.dto.*;
import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.entity.UserStatus;
import com.slit.realityvote.service.ComplianceMonitoringService;
import com.slit.realityvote.service.UserService;
import com.slit.realityvote.service.VotingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Compliance Monitoring Dashboard controller.
 * Serves the main page (Thymeleaf) and JSON endpoints for AJAX/polling.
 *
 * All endpoints require COMPLIANCE_OFFICER or ADMINISTRATOR role.
 */
@Controller
@RequestMapping("/compliance/monitoring")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'COMPLIANCE_OFFICER')")
@RequiredArgsConstructor
public class ComplianceMonitoringController {

    private final ComplianceMonitoringService monitoringService;
    private final UserService userService;

    // ── Main page ─────────────────────────────────────────────────────────────

    @GetMapping
    public String monitoringDashboard(
            @RequestParam(required = false) Long sessionId,
            Model model) {

        List<MonitoringSessionInfo> sessions = monitoringService.getAllSessions();
        model.addAttribute("sessions", sessions);

        if (sessionId != null) {
            MonitoringSessionInfo selected = monitoringService.getSessionInfo(sessionId);
            model.addAttribute("selectedSession", selected);
            model.addAttribute("selectedSessionId", sessionId);
        } else if (!sessions.isEmpty()) {
            // Default to first (most recent) session
            MonitoringSessionInfo first = sessions.get(0);
            model.addAttribute("selectedSession", first);
            model.addAttribute("selectedSessionId", first.id());
        }

        return "compliance/monitoring";
    }

    // ── JSON endpoints for AJAX polling ─────────────────────────────────────

    @GetMapping("/{sessionId}/vote-trend")
    @ResponseBody
    public ResponseEntity<List<VoteTrendPoint>> getVoteTrend(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "5") int intervalMin) {
        return ResponseEntity.ok(monitoringService.getVoteTrend(sessionId, intervalMin));
    }

    @GetMapping("/{sessionId}/user-trend")
    @ResponseBody
    public ResponseEntity<List<UserCountPoint>> getUserTrend(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "5") int intervalMin) {
        return ResponseEntity.ok(monitoringService.getUserCountTrend(sessionId, intervalMin));
    }

    @GetMapping("/{sessionId}/activity")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getActivityFeed(
            @PathVariable Long sessionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "60") int minutesAgo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        AuditEventType et = null;
        if (eventType != null && !eventType.isBlank()) {
            try { et = AuditEventType.valueOf(eventType); } catch (IllegalArgumentException ignored) {}
        }

        Page<UserActivityRow> result = monitoringService.getUserActivityFeed(
                sessionId, keyword, et, minutesAgo, page, size);

        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "currentPage", result.getNumber()
        ));
    }

    @GetMapping("/user/profile")
    @ResponseBody
    public ResponseEntity<?> getUserProfile(@RequestParam String email) {
        try {
            UserComplianceProfile profile = monitoringService.getUserProfile(email);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Compliance actions ───────────────────────────────────────────────────

    @PostMapping("/user/{userId}/flag")
    @ResponseBody
    public ResponseEntity<ComplianceActionResult> flagUser(
            @PathVariable Long userId,
            @RequestBody FlagUserRequest req,
            Authentication auth) {
        try {
            var user = userService.flagUser(userId, req.reason(), req.notes(), auth.getName());
            return ResponseEntity.ok(new ComplianceActionResult(true,
                    "User " + user.getEmail() + " has been flagged.", UserStatus.FLAGGED));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ComplianceActionResult(false, e.getMessage(), null));
        }
    }

    @PostMapping("/user/{userId}/unflag")
    @ResponseBody
    public ResponseEntity<ComplianceActionResult> unflagUser(
            @PathVariable Long userId,
            @RequestBody FlagUserRequest req,
            Authentication auth) {
        try {
            var user = userService.unflagUser(userId, req.reason(), auth.getName());
            return ResponseEntity.ok(new ComplianceActionResult(true,
                    "User " + user.getEmail() + " has been unflagged.", UserStatus.ACTIVE));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ComplianceActionResult(false, e.getMessage(), null));
        }
    }

    @PostMapping("/user/{userId}/warn")
    @ResponseBody
    public ResponseEntity<ComplianceActionResult> warnUser(
            @PathVariable Long userId,
            @RequestBody FlagUserRequest req,
            Authentication auth) {
        try {
            var user = userService.warnUser(userId, req.reason(), req.notes(), auth.getName());
            return ResponseEntity.ok(new ComplianceActionResult(true,
                    "Warning issued to " + user.getEmail() + ".", UserStatus.WARNING));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ComplianceActionResult(false, e.getMessage(), null));
        }
    }

    @PostMapping("/user/{userId}/message")
    @ResponseBody
    public ResponseEntity<ComplianceActionResult> sendMessage(
            @PathVariable Long userId,
            @RequestBody SendMessageRequest req,
            Authentication auth) {
        try {
            userService.sendComplianceMessage(userId, req.subject(), req.body(), auth.getName());
            return ResponseEntity.ok(new ComplianceActionResult(true,
                    "Message sent successfully.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new ComplianceActionResult(false, e.getMessage(), null));
        }
    }
}
