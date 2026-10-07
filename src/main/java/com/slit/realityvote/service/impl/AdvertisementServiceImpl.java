package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.AdvertisementRepository;
import com.slit.realityvote.repository.AdvertisingCampaignRepository;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.repository.VotingSessionRepository;
import com.slit.realityvote.service.AdvertisementService;
import com.slit.realityvote.service.AuditLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvertisementServiceImpl implements AdvertisementService {

    private final AdvertisementRepository adRepository;
    private final AdvertisingCampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final VotingSessionRepository votingSessionRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Advertisement create(Advertisement ad, Long campaignId, String officerEmail) {
        return createWithSessions(ad, campaignId, null, officerEmail);
    }

    @Override
    @Transactional
    public Advertisement createWithSessions(Advertisement ad, Long campaignId, List<Long> sessionIds, String officerEmail) {
        AdvertisingCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + campaignId));

        if (campaign.getStatus() == CampaignStatus.COMPLETED || campaign.getStatus() == CampaignStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot add advertisements to a " + campaign.getStatus() + " campaign.");
        }

        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + officerEmail));

        ad.setCampaign(campaign);
        ad.setCreatedBy(officer);
        ad.setStatus(AdvertisementStatus.DRAFT);

        if (sessionIds != null && !sessionIds.isEmpty()) {
            List<VotingSession> sessions = votingSessionRepository.findAllById(sessionIds);
            ad.setSessions(new ArrayList<>(sessions));
        }

        Advertisement saved = adRepository.save(ad);

        auditLogService.record(AuditEventType.ADVERTISEMENT_CREATED,
                "Advertisement '" + saved.getTitle() + "' created in campaign '" + campaign.getName()
                        + "' with " + (ad.getSessions() != null ? ad.getSessions().size() : 0) + " target session(s) by " + officerEmail,
                officerEmail);
        return saved;
    }

    @Override
    @Transactional
    public Advertisement update(Long id, Advertisement updated) {
        return updateWithSessions(id, updated, null);
    }

    @Override
    @Transactional
    public Advertisement updateWithSessions(Long id, Advertisement updated, List<Long> sessionIds) {
        Advertisement existing = getById(id);
        if (existing.getStatus() != AdvertisementStatus.DRAFT && existing.getStatus() != AdvertisementStatus.REJECTED) {
            throw new IllegalStateException("Only DRAFT or REJECTED advertisements can be edited.");
        }
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setImageUrl(updated.getImageUrl());
        existing.setTargetUrl(updated.getTargetUrl());

        if (sessionIds != null) {
            List<VotingSession> sessions = votingSessionRepository.findAllById(sessionIds);
            existing.getSessions().clear();
            existing.getSessions().addAll(sessions);
        }

        return adRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id, String email) {
        Advertisement ad = getById(id);
        if (ad.getStatus() != AdvertisementStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT advertisements can be deleted.");
        }
        adRepository.delete(ad);
    }

    @Override
    @Transactional
    public Advertisement submitForReview(Long id, String email) {
        Advertisement ad = getById(id);
        if (ad.getStatus() != AdvertisementStatus.DRAFT && ad.getStatus() != AdvertisementStatus.REJECTED) {
            throw new IllegalStateException("Only DRAFT or REJECTED advertisements can be submitted for review.");
        }
        ad.setReviewedBy(null);
        ad.setReviewedAt(null);
        ad.setComplianceComment(null);
        ad.setStatus(AdvertisementStatus.PENDING_REVIEW);
        adRepository.save(ad);

        auditLogService.record(AuditEventType.ADVERTISEMENT_SUBMITTED,
                "Advertisement '" + ad.getTitle() + "' submitted for compliance review by " + email, email);
        return ad;
    }

    @Override
    @Transactional
    public Advertisement approve(Long id, String comment, String reviewerEmail) {
        return approveWithSessions(id, comment, null, reviewerEmail);
    }

    @Override
    @Transactional
    public Advertisement approveWithSessions(Long id, String comment, List<Long> sessionIds, String reviewerEmail) {
        return approveWithSchedule(id, comment, sessionIds, null, null, reviewerEmail);
    }

    @Override
    @Transactional
    public Advertisement approveWithSchedule(Long id, String comment, List<Long> sessionIds,
                                              LocalDateTime displayStartTime, LocalDateTime displayEndTime,
                                              String reviewerEmail) {
        Advertisement ad = getById(id);
        if (ad.getStatus() != AdvertisementStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only PENDING_REVIEW advertisements can be approved.");
        }
        if (ad.getCreatedBy().getEmail().equalsIgnoreCase(reviewerEmail)) {
            throw new IllegalStateException("You cannot approve your own advertisement.");
        }

        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new EntityNotFoundException("Reviewer not found: " + reviewerEmail));

        if (sessionIds != null) {
            List<VotingSession> sessions = votingSessionRepository.findAllById(sessionIds);
            ad.getSessions().clear();
            ad.getSessions().addAll(sessions);
        }

        if (displayStartTime != null || displayEndTime != null) {
            if (displayStartTime != null && displayEndTime != null && displayEndTime.isBefore(displayStartTime)) {
                throw new IllegalArgumentException("Display end time cannot be before start time.");
            }
            ad.setDisplayStartTime(displayStartTime);
            ad.setDisplayEndTime(displayEndTime);
        }

        ad.setStatus(AdvertisementStatus.APPROVED);
        ad.setReviewedBy(reviewer);
        ad.setReviewedAt(LocalDateTime.now());
        ad.setComplianceComment(comment);
        ad.setScheduledBy(reviewer);
        ad.setScheduledAt(LocalDateTime.now());
        adRepository.save(ad);

        auditLogService.record(AuditEventType.ADVERTISEMENT_APPROVED,
                "Advertisement '" + ad.getTitle() + "' approved by " + reviewerEmail +
                        " for " + ad.getSessions().size() + " session(s)" +
                        (comment != null && !comment.isBlank() ? ". Comment: " + comment : ""),
                reviewerEmail);
        return ad;
    }

    @Override
    @Transactional
    public Advertisement reject(Long id, String comment, String reviewerEmail) {
        Advertisement ad = getById(id);
        if (ad.getStatus() != AdvertisementStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only PENDING_REVIEW advertisements can be rejected.");
        }
        if (ad.getCreatedBy().getEmail().equalsIgnoreCase(reviewerEmail)) {
            throw new IllegalStateException("You cannot reject your own advertisement.");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }

        User reviewer = userRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new EntityNotFoundException("Reviewer not found: " + reviewerEmail));

        ad.setStatus(AdvertisementStatus.REJECTED);
        ad.setReviewedBy(reviewer);
        ad.setReviewedAt(LocalDateTime.now());
        ad.setComplianceComment(comment);
        adRepository.save(ad);

        auditLogService.record(AuditEventType.ADVERTISEMENT_REJECTED,
                "Advertisement '" + ad.getTitle() + "' rejected by " + reviewerEmail + ". Reason: " + comment,
                reviewerEmail);
        return ad;
    }

    @Override
    @Transactional
    public Advertisement activate(Long id, String email) {
        Advertisement ad = getById(id);
        if (ad.getStatus() != AdvertisementStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED advertisements can be activated.");
        }
        ad.setStatus(AdvertisementStatus.ACTIVE);
        adRepository.save(ad);

        auditLogService.record(AuditEventType.ADVERTISEMENT_ACTIVATED,
                "Advertisement '" + ad.getTitle() + "' activated by " + email, email);
        return ad;
    }

    @Override
    public Advertisement getById(Long id) {
        return adRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Advertisement not found: " + id));
    }

    @Override
    public List<Advertisement> getAll() {
        return adRepository.findAllByOrderByCreatedDateDesc();
    }

    @Override
    public List<Advertisement> getByStatus(AdvertisementStatus status) {
        return adRepository.findByStatusOrderByCreatedDateDesc(status);
    }

    @Override
    public long countByStatus(AdvertisementStatus status) {
        return adRepository.countByStatus(status);
    }

    @Override
    public List<Advertisement> getByCampaign(Long campaignId) {
        return adRepository.findByCampaign_IdOrderByCreatedDateDesc(campaignId);
    }

    @Override
    @Transactional
    public Advertisement assignToSessions(Long adId, List<Long> sessionIds,
                                           LocalDateTime displayStartTime, LocalDateTime displayEndTime,
                                           String email) {
        Advertisement ad = getById(adId);
        if (ad.getStatus() != AdvertisementStatus.APPROVED && ad.getStatus() != AdvertisementStatus.ACTIVE) {
            throw new IllegalStateException("Only APPROVED or ACTIVE advertisements can be scheduled.");
        }
        if (displayStartTime != null && displayEndTime != null && displayEndTime.isBefore(displayStartTime)) {
            throw new IllegalArgumentException("Display end time cannot be before display start time.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));

        List<VotingSession> sessions = votingSessionRepository.findAllById(sessionIds);
        ad.getSessions().clear();
        ad.getSessions().addAll(sessions);
        ad.setDisplayStartTime(displayStartTime);
        ad.setDisplayEndTime(displayEndTime);
        ad.setScheduledBy(user);
        ad.setScheduledAt(LocalDateTime.now());
        adRepository.save(ad);

        auditLogService.record(AuditEventType.ADVERTISEMENT_SCHEDULED,
                "Advertisement '" + ad.getTitle() + "' scheduled to " + sessions.size()
                        + " session(s) by " + email
                        + (displayStartTime != null ? " from " + displayStartTime : "")
                        + (displayEndTime != null ? " until " + displayEndTime : ""),
                email);
        return ad;
    }

    @Override
    @Transactional
    public Advertisement removeFromSession(Long adId, Long sessionId, String email) {
        Advertisement ad = getById(adId);
        ad.getSessions().removeIf(s -> s.getId().equals(sessionId));
        adRepository.save(ad);
        return ad;
    }

    @Override
    public List<Advertisement> getActiveAdsForSession(Long sessionId) {
        return adRepository.findActiveOrApprovedAdsBySessionWithinWindow(sessionId, LocalDateTime.now());
    }

    @Override
    public List<Advertisement> getAdsForOpenSessions() {
        return adRepository.findActiveOrApprovedAdsForOpenSessions(LocalDateTime.now());
    }
}
