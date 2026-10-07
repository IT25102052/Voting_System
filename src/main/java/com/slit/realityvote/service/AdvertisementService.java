package com.slit.realityvote.service;

import com.slit.realityvote.entity.Advertisement;
import com.slit.realityvote.entity.AdvertisementStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementService {

    Advertisement create(Advertisement ad, Long campaignId, String officerEmail);

    Advertisement createWithSessions(Advertisement ad, Long campaignId, List<Long> sessionIds, String officerEmail);

    Advertisement update(Long id, Advertisement updated);

    Advertisement updateWithSessions(Long id, Advertisement updated, List<Long> sessionIds);

    void delete(Long id, String email);

    Advertisement submitForReview(Long id, String email);

    Advertisement approve(Long id, String comment, String reviewerEmail);

    Advertisement approveWithSessions(Long id, String comment, List<Long> sessionIds, String reviewerEmail);

    Advertisement approveWithSchedule(Long id, String comment, List<Long> sessionIds,
                                      LocalDateTime displayStartTime, LocalDateTime displayEndTime,
                                      String reviewerEmail);

    Advertisement reject(Long id, String comment, String reviewerEmail);

    Advertisement activate(Long id, String email);

    Advertisement getById(Long id);

    List<Advertisement> getAll();

    List<Advertisement> getByStatus(AdvertisementStatus status);

    long countByStatus(AdvertisementStatus status);

    List<Advertisement> getByCampaign(Long campaignId);

    Advertisement assignToSessions(Long adId, List<Long> sessionIds,
                                    LocalDateTime displayStartTime, LocalDateTime displayEndTime,
                                    String email);

    Advertisement removeFromSession(Long adId, Long sessionId, String email);

    List<Advertisement> getActiveAdsForSession(Long sessionId);

    List<Advertisement> getAdsForOpenSessions();
}
