package com.slit.realityvote.service.impl;

import com.slit.realityvote.entity.*;
import com.slit.realityvote.repository.AdvertisingCampaignRepository;
import com.slit.realityvote.repository.UserRepository;
import com.slit.realityvote.service.AdvertisingCampaignService;
import com.slit.realityvote.service.AuditLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvertisingCampaignServiceImpl implements AdvertisingCampaignService {

    private final AdvertisingCampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AdvertisingCampaign create(AdvertisingCampaign campaign, String officerEmail) {
        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + officerEmail));
        campaign.setCreatedBy(officer);
        campaign.setStatus(CampaignStatus.DRAFT);
        AdvertisingCampaign saved = campaignRepository.save(campaign);
        auditLogService.record(AuditEventType.CAMPAIGN_CREATED,
                "Campaign '" + saved.getName() + "' created by " + officerEmail, officerEmail);
        return saved;
    }

    @Override
    @Transactional
    public AdvertisingCampaign update(Long id, AdvertisingCampaign updated) {
        AdvertisingCampaign existing = getById(id);
        if (existing.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT campaigns can be edited.");
        }
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        return campaignRepository.save(existing);
    }

    @Override
    @Transactional
    public AdvertisingCampaign activate(Long id, String email) {
        AdvertisingCampaign campaign = getById(id);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT campaigns can be activated.");
        }
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaignRepository.save(campaign);
        auditLogService.record(AuditEventType.CAMPAIGN_ACTIVATED,
                "Campaign '" + campaign.getName() + "' activated by " + email, email);
        return campaign;
    }

    @Override
    @Transactional
    public AdvertisingCampaign complete(Long id, String email) {
        AdvertisingCampaign campaign = getById(id);
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE campaigns can be marked completed.");
        }
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaignRepository.save(campaign);
        auditLogService.record(AuditEventType.CAMPAIGN_COMPLETED,
                "Campaign '" + campaign.getName() + "' completed by " + email, email);
        return campaign;
    }

    @Override
    @Transactional
    public AdvertisingCampaign archive(Long id, String email) {
        AdvertisingCampaign campaign = getById(id);
        campaign.setStatus(CampaignStatus.ARCHIVED);
        campaignRepository.save(campaign);
        auditLogService.record(AuditEventType.CAMPAIGN_COMPLETED,
                "Campaign '" + campaign.getName() + "' archived by " + email, email);
        return campaign;
    }

    @Override
    public AdvertisingCampaign getById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found: " + id));
    }

    @Override
    public List<AdvertisingCampaign> getAll() {
        return campaignRepository.findAllByOrderByCreatedDateDesc();
    }

    @Override
    public List<AdvertisingCampaign> getByUser(Long userId) {
        return campaignRepository.findByCreatedBy_IdOrderByCreatedDateDesc(userId);
    }

    @Override
    public long countActive() {
        return campaignRepository.countByStatus(CampaignStatus.ACTIVE);
    }
}
