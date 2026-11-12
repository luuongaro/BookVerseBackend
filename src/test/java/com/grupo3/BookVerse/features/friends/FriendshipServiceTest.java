package com.grupo3.BookVerse.features.friends;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendshipServiceTest {
    FriendshipRepository repo; UserRepository users; FriendshipService service;
    UserEntity sender, recipient, outsider; UUID requestId;
    @BeforeEach void setup() {
        repo = mock(FriendshipRepository.class); users = mock(UserRepository.class); service = new FriendshipService(repo, users);
        sender = UserEntity.builder().id(1L).idExternal(UUID.randomUUID()).username("sender").status(UserStatus.ACTIVE).build();
        recipient = UserEntity.builder().id(2L).idExternal(UUID.randomUUID()).username("recipient").status(UserStatus.ACTIVE).build();
        outsider = UserEntity.builder().id(3L).idExternal(UUID.randomUUID()).username("outsider").status(UserStatus.ACTIVE).build(); requestId = UUID.randomUUID();
        authenticate(sender);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void authenticate(UserEntity u) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of())); }
    Friendship pending() { var f = new Friendship(); f.setIdExternal(requestId); f.setUserLow(sender); f.setUserHigh(recipient); f.setRequester(sender); f.setStatus(Friendship.Status.PENDING); return f; }
    @Test void rejectsSelfAndDuplicate() {
        when(users.findByIdExternalAndStatus(sender.getIdExternal(), UserStatus.ACTIVE)).thenReturn(Optional.of(sender));
        assertThrows(BadRequestException.class, () -> service.request(sender.getIdExternal()));
        when(users.findByIdExternalAndStatus(recipient.getIdExternal(), UserStatus.ACTIVE)).thenReturn(Optional.of(recipient));
        when(users.lockActiveUser(1L)).thenReturn(Optional.of(sender)); when(users.lockActiveUser(2L)).thenReturn(Optional.of(recipient));
        when(repo.findPairForUpdate(1L, 2L)).thenReturn(Optional.of(pending()));
        assertThrows(DuplicateResourceException.class, () -> service.request(recipient.getIdExternal()));
        verify(repo, never()).save(any());
    }
    @Test void onlyRecipientCanAccept() {
        var f = pending(); when(repo.findByExternalForUpdate(requestId)).thenReturn(Optional.of(f));
        assertThrows(AccessDeniedException.class, () -> service.respond(requestId, true));
        authenticate(outsider); assertThrows(AccessDeniedException.class, () -> service.respond(requestId, true));
        authenticate(recipient); service.respond(requestId, true); assertEquals(Friendship.Status.ACCEPTED, f.getStatus());
        assertThrows(BadRequestException.class, () -> service.respond(requestId, true));
    }
    @Test void onlyRecipientCanRejectAndOnlySenderCanCancel() {
        var f = pending(); when(repo.findByExternalForUpdate(requestId)).thenReturn(Optional.of(f));
        authenticate(recipient); assertThrows(AccessDeniedException.class, () -> service.remove(requestId));
        service.respond(requestId, false); verify(repo).delete(f);
    }
}
