package com.grupo3.BookVerse.auth.filters;

import com.grupo3.BookVerse.auth.jwt.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String email = jwtService.extractEmail(jwt);

            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (email != null && authentication == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

            if (!userDetails.isEnabled()) {
                    writeUnauthorizedResponse(
                            response,
                            request,
                            "User account is inactive"
                    );
                    return;
                }

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);
                }
            }

        } catch (JwtException exception) {
            writeUnauthorizedResponse(response, request, "Invalid or expired JWT token");
            return;

        } catch (Exception exception) {
            writeUnauthorizedResponse(response, request, "Authentication error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            String message
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        response.getWriter().write(
                String.format(
                        """
                        {
                            "error":"%s",
                            "status":%d,
                            "path":"%s"
                        }
                        """,
                        message,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        request.getRequestURI()
                )
        );

        response.getWriter().flush();
    }
}