package com.grupo3.BookVerse.auth;

import com.grupo3.BookVerse.auth.controller.PasswordRecoveryController;
import com.grupo3.BookVerse.auth.entrypoint.*;
import com.grupo3.BookVerse.auth.filters.JwtAuthenticationFilter;
import com.grupo3.BookVerse.auth.jwt.JwtService;
import com.grupo3.BookVerse.auth.service.PasswordRecoveryService;
import com.grupo3.BookVerse.common.exception.*;
import com.grupo3.BookVerse.config.security.SecurityConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(PasswordRecoveryControllerTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {"jwt.secret=password-recovery-controller-test-key-32-bytes", "jwt.expiration=3600000"})
class PasswordRecoveryControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired PasswordRecoveryService recovery;
    MockMvc mvc;
    final String token = "a".repeat(43);

    @BeforeEach void setup() {
        reset(recovery);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void anonymousUsersCanRequestALinkAndTheResponseDoesNotRevealTheAccount() throws Exception {
        for (String email : new String[]{"reader@example.com", "unknown@example.com"}) {
            mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\"}"))
                    .andExpect(status().isAccepted()).andExpect(content().string(""))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
        mvc.perform(get("/api/roles")).andExpect(status().isUnauthorized());
    }

    @Test void invalidEmailsAreRejectedBeforeAnyRecoveryWork() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(recovery);
    }

    @Test void aFullQueueKeepsTheSameGenericResponse() throws Exception {
        doThrow(new TaskRejectedException("full")).when(recovery).requestReset(anyString());
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reader@example.com\"}"))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
    }

    @Test void resettingRequiresAValidLinkAndPasswordButNotALoginSession() throws Exception {
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+token+"\",\"password\":\"Updated123!\"}"))
                .andExpect(status().isNoContent()).andExpect(header().string("Cache-Control", "no-store"));
        verify(recovery).resetPassword(token, "Updated123!");
    }

    @Test void expiredOrConsumedTokensHaveTheSameSafeError() throws Exception {
        doThrow(new InvalidPasswordResetTokenException()).when(recovery).resetPassword(anyString(), anyString());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\""+token+"\",\"password\":\"Updated123!\"}"))
                .andExpect(status().isGone()).andExpect(jsonPath("$.status").value(410))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test void rejectsWeakPasswordsMalformedTokensAndInputsBeyondBcryptCapacity() throws Exception {
        for (String password : new String[]{"weak", "missingNumber!", "A1!" + "é".repeat(36), "A1!" + "a".repeat(70)}) {
            mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\""+token+"\",\"password\":\""+password+"\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"short\",\"password\":\"Updated123!\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(recovery);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, PasswordRecoveryController.class, GlobalExceptionHandler.class,
            RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
    static class Config {
        @Bean PasswordRecoveryService recovery() { return mock(PasswordRecoveryService.class); }
        @Bean JwtService jwt() { return mock(JwtService.class); }
        @Bean UserDetailsService users() { return mock(UserDetailsService.class); }
        @Bean JwtAuthenticationFilter filter(JwtService jwt, UserDetailsService users) { return new JwtAuthenticationFilter(jwt, users); }
    }
}
