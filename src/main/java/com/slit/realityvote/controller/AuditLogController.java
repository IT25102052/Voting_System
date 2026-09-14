package com.slit.realityvote.controller;

import com.slit.realityvote.entity.AuditEventType;
import com.slit.realityvote.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Read-only for the Compliance Officer (and Administrator) - matches
 * "Only authorized administrators and compliance officers should be
 * able to access detailed audit logs." There is deliberately no
 * edit/delete endpoint anywhere in this controller: an audit trail that
 * can be altered isn't an audit trail.
 */
@Controller
@RequestMapping("/compliance/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private static final int PAGE_SIZE = 15;

    @GetMapping
    public String list(@RequestParam(required = false) AuditEventType eventType,
                        @RequestParam(defaultValue = "false") boolean flaggedOnly,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {

        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdDate").descending());
        var results = auditLogService.search(eventType, flaggedOnly, keyword, pageable);

        model.addAttribute("logPage", results);
        model.addAttribute("eventType", eventType);
        model.addAttribute("flaggedOnly", flaggedOnly);
        model.addAttribute("keyword", keyword);
        model.addAttribute("eventTypes", AuditEventType.values());
        model.addAttribute("flaggedCount", auditLogService.countFlagged());
        return "audit/list";
    }
}
