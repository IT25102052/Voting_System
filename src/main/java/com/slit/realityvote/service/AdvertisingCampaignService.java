package com.slit.realityvote.service;

import com.slit.realityvote.entity.AdvertisingCampaign;

import java.util.List;

public interface AdvertisingCampaignService {

    AdvertisingCampaign create(AdvertisingCampaign campaign, String officerEmail);

    AdvertisingCampaign update(Long id, AdvertisingCampaign updated);

    AdvertisingCampaign activate(Long id, String email);

    AdvertisingCampaign complete(Long id, String email);

    AdvertisingCampaign archive(Long id, String email);

    AdvertisingCampaign getById(Long id);

    List<AdvertisingCampaign> getAll();

    List<AdvertisingCampaign> getByUser(Long userId);

    long countActive();
}
