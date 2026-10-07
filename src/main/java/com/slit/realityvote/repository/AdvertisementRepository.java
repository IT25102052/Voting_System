package com.slit.realityvote.repository;

import com.slit.realityvote.entity.Advertisement;
import com.slit.realityvote.entity.AdvertisementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    List<Advertisement> findAllByOrderByCreatedDateDesc();

    List<Advertisement> findByCampaign_IdOrderByCreatedDateDesc(Long campaignId);

    List<Advertisement> findByStatusOrderByCreatedDateDesc(AdvertisementStatus status);

    long countByStatus(AdvertisementStatus status);

    List<Advertisement> findByCreatedBy_IdOrderByCreatedDateDesc(Long userId);

    /**
     * Active or Approved advertisements assigned to a specific voting session,
     * filtered by display window. Displays as soon as Compliance approves.
     */
    @Query("SELECT DISTINCT a FROM Advertisement a JOIN a.sessions s " +
           "WHERE s.id = :sessionId " +
           "AND (a.status = 'APPROVED' OR a.status = 'ACTIVE') " +
           "AND (a.displayStartTime IS NULL OR a.displayStartTime <= :now) " +
           "AND (a.displayEndTime IS NULL OR a.displayEndTime >= :now) " +
           "ORDER BY a.createdDate DESC")
    List<Advertisement> findActiveOrApprovedAdsBySessionWithinWindow(
            @Param("sessionId") Long sessionId,
            @Param("now") LocalDateTime now);

    /**
     * Active or Approved advertisements for any currently OPEN voting session.
     */
    @Query("SELECT DISTINCT a FROM Advertisement a JOIN a.sessions s " +
           "WHERE s.status = 'OPEN' " +
           "AND (a.status = 'APPROVED' OR a.status = 'ACTIVE') " +
           "AND (a.displayStartTime IS NULL OR a.displayStartTime <= :now) " +
           "AND (a.displayEndTime IS NULL OR a.displayEndTime >= :now) " +
           "ORDER BY a.createdDate DESC")
    List<Advertisement> findActiveOrApprovedAdsForOpenSessions(@Param("now") LocalDateTime now);
}
