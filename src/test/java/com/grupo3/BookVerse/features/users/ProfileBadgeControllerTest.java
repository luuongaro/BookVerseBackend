package com.grupo3.BookVerse.features.users;

import com.grupo3.BookVerse.features.users.controller.ProfileBadgeController;
import com.grupo3.BookVerse.features.users.domain.ProfileBadgeType;
import com.grupo3.BookVerse.features.users.service.ProfileBadgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(ProfileBadgeControllerTest.Config.class)
@WebAppConfiguration
class ProfileBadgeControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired ProfileBadgeService service;
    MockMvc mvc;
    final UUID id = UUID.fromString("11111111-1111-4111-8111-111111111111");
    final String path = "/api/users/profiles/" + id + "/badge";
    @BeforeEach void setup() { reset(service); mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void readersAndModeratorsCanViewButCannotAssign() throws Exception {
        when(service.get(id)).thenReturn(ProfileBadgeType.FOUNDING_WRITER);
        for (String role : new String[]{"USER", "MODERATOR"}) {
            mvc.perform(get(path).with(user("viewer").roles(role))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.badge").value("FOUNDING_WRITER"));
            mvc.perform(put(path).with(user("viewer").roles(role)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"badge\":\"FOUNDING_READER_1\"}")).andExpect(status().isForbidden());
        }
        verify(service, never()).update(any(), any());
    }

    @Test void onlyAdminCanAssignAndRemoveAndInvalidBadgeIsRejected() throws Exception {
        when(service.update(id, ProfileBadgeType.FOUNDING_READER_1)).thenReturn(ProfileBadgeType.FOUNDING_READER_1);
        mvc.perform(put(path).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"badge\":\"FOUNDING_READER_1\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.badge").value("FOUNDING_READER_1"));
        mvc.perform(put(path).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"badge\":null}")).andExpect(status().isOk());
        mvc.perform(put(path).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"badge\":\"INVALID\"}")).andExpect(status().isBadRequest());
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        verify(service).update(id, null);
    }
    @Configuration @EnableWebMvc @EnableWebSecurity @EnableMethodSecurity
    static class Config {
        @Bean ProfileBadgeService service() { return mock(ProfileBadgeService.class); }
        @Bean ProfileBadgeController controller(ProfileBadgeService service) { return new ProfileBadgeController(service); }
        @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults()).build();
        }
    }
}
