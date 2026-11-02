package com.grupo3.BookVerse.config.security;

import com.grupo3.BookVerse.auth.entrypoint.RestAccessDeniedHandler;
import com.grupo3.BookVerse.auth.entrypoint.RestAuthenticationEntryPoint;
import com.grupo3.BookVerse.auth.filters.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.DefaultCorsProcessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RenderCorsTest {
    private final SecurityConfig security = new SecurityConfig(
            mock(JwtAuthenticationFilter.class),
            mock(RestAuthenticationEntryPoint.class),
            mock(RestAccessDeniedHandler.class));

    private MockHttpServletResponse preflight(String allowed, String origin) throws Exception {
        var request = new MockHttpServletRequest("OPTIONS", "/api/books");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "GET");
        request.addHeader("Access-Control-Request-Headers", "Authorization, Content-Type");
        var response = new MockHttpServletResponse();
        var source = security.corsConfigurationSource(allowed);
        new DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response);
        return response;
    }

    @Test
    void allowsAuthenticatedBrowserRequestsFromBothBookVerseDomains() throws Exception {
        var origins = "https://www.bookverse.com.ar, https://bookverse.com.ar, ";
        for (var origin : new String[]{"https://www.bookverse.com.ar", "https://bookverse.com.ar"}) {
            var response = preflight(origins, origin);
            assertEquals(200, response.getStatus());
            assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
            assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("Authorization"));
        }
    }

    @Test
    void rejectsUnlistedAndLookalikeDomains() throws Exception {
        for (var origin : new String[]{"https://www.bookverse.com.ar.evil.example", "http://localhost:4200"}) {
            var response = preflight("https://www.bookverse.com.ar", origin);
            assertEquals(403, response.getStatus());
            assertNull(response.getHeader("Access-Control-Allow-Origin"));
        }
    }

    @Test
    void continuesToSupportTheLocalDevelopmentOrigin() throws Exception {
        assertEquals(200, preflight("http://localhost:4200", "http://localhost:4200").getStatus());
    }

}
