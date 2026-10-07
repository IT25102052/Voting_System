package com.slit.realityvote.controller;

import com.slit.realityvote.service.AdvertisementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AdvertisementService advertisementService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredAds", advertisementService.getAdsForOpenSessions());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
