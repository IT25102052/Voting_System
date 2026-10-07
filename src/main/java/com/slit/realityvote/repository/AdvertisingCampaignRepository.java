package com.slit.realityvote.repository;

import com.slit.realityvote.entity.AdvertisingCampaign;
import com.slit.realityvote.entity.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvertisingCampaignRepository extends JpaRepository<AdvertisingCampaign, Long> {

    List<AdvertisingCampaign> findByCreatedBy_IdOrderByCreatedDateDesc(Long userId);

    List<AdvertisingCampaign> findAllByOrderByCreatedDateDesc();

    long countByStatus(CampaignStatus status);
}
