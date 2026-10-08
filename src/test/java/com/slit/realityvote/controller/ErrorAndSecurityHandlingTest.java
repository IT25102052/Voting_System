package com.slit.realityvote.controller;

import com.slit.realityvote.aspect.AuditLoggingAspect;
import com.slit.realityvote.config.GlobalExceptionHandler;
import com.slit.realityvote.config.SecurityConfig;
import com.slit.realityvote.security.DatabaseUserDetailsService;
import com.slit.realityvote.service.AdvertisementService;
import com.slit.realityvote.service.FileStorageService;
import com.slit.realityvote.service.RealityShowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {HomeController.class, RealityShowController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ErrorAndSecurityHandlingTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdvertisementService advertisementService;
    @MockBean RealityShowService showService;
    @MockBean FileStorageService fileStorageService;
    @MockBean DatabaseUserDetailsService userDetailsService;
    @MockBean AuditLoggingAspect auditLoggingAspect;

    @Test
    void getError403_returnsAccessDeniedPage() throws Exception {
        mockMvc.perform(get("/error/403"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
    }

    @Test
    void postError403_supportedAndReturnsAccessDeniedPage() throws Exception {
        mockMvc.perform(post("/error/403").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void forbiddenPostAjax_returnsJsonForbiddenResponse() throws Exception {
        mockMvc.perform(post("/admin/shows")
                        .with(csrf())
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
