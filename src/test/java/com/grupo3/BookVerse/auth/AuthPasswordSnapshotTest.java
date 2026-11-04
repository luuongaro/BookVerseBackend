package com.grupo3.BookVerse.auth;

import com.grupo3.BookVerse.auth.dtos.AuthRequest;
import com.grupo3.BookVerse.auth.jwt.JwtService;
import com.grupo3.BookVerse.auth.service.AuthServiceImpl;
import com.grupo3.BookVerse.features.subscriptions.repository.SubscriptionRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.features.users.service.UserService;
import com.grupo3.BookVerse.infrastructure.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthPasswordSnapshotTest {
    @Test void aConcurrentPasswordResetCannotUpgradeAnOldPasswordLoginToANewSessionVersion() {
        var authentication = mock(AuthenticationManager.class);
        var jwt = mock(JwtService.class);
        var users = mock(UserRepository.class);
        var id = UUID.randomUUID();
        var authenticated = UserEntity.builder().idExternal(id).email("reader@example.com").credentialsVersion(0).build();
        var afterReset = UserEntity.builder().idExternal(id).email("reader@example.com").credentialsVersion(1).build();
        when(authentication.authenticate(any())).thenReturn(new UsernamePasswordAuthenticationToken(authenticated, null, List.of()));
        when(users.findByEmailWithRoles("reader@example.com")).thenReturn(Optional.of(afterReset));
        when(jwt.generateToken(authenticated)).thenReturn("session-from-authenticated-snapshot");
        var service = new AuthServiceImpl(authentication, jwt, users, mock(UserService.class),
                mock(SubscriptionRepository.class), mock(EmailService.class));
        var response = service.authenticate(new AuthRequest("reader@example.com", "Previous1!"));
        assertEquals("session-from-authenticated-snapshot", response.token());
        assertEquals(id, response.userId());
        verify(jwt).generateToken(authenticated);
        verify(jwt, never()).generateToken(afterReset);
    }
}
