package com.grupo3.BookVerse.features.users;

import com.grupo3.BookVerse.features.users.controller.UserProfileController;
import com.grupo3.BookVerse.features.users.dto.UserProfileResponseDto;
import com.grupo3.BookVerse.features.users.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(UserProfileControllerTest.Config.class)
@WebAppConfiguration
class UserProfileControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired UserProfileService service;
    MockMvc mvc;
    final UUID id = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @BeforeEach void setup() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void anonymousRequestsAreRejected() throws Exception {
        for (var path : List.of("/api/users/profiles", "/api/users/profiles/" + id, "/api/users/profiles/" + id + "/stories")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }

    @Test void regularUsersCanSearchAndResponsesContainOnlyPublicIdentity() throws Exception {
        when(service.search("Sarah", PageRequest.of(0, 24)))
                .thenReturn(new PageImpl<>(List.of(new UserProfileResponseDto(id, "Sarah")), PageRequest.of(0, 24), 1));
        mvc.perform(get("/api/users/profiles").with(user("reader").roles("USER"))
                        .param("username", "Sarah").param("page", "-1").param("size", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("Sarah"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].roles").doesNotExist())
                .andExpect(jsonPath("$.content[0].subscription").doesNotExist());
    }

    @Test void regularUsersCanVisitAnotherProfile() throws Exception {
        when(service.getProfile(id)).thenReturn(new UserProfileResponseDto(id, "Sarah"));
        mvc.perform(get("/api/users/profiles/" + id).with(user("reader").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("Sarah"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class Config {
        @Bean UserProfileService service() { return mock(UserProfileService.class); }
        @Bean UserProfileController controller(UserProfileService service) { return new UserProfileController(service); }
        @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults()).build();
        }
    }
}
