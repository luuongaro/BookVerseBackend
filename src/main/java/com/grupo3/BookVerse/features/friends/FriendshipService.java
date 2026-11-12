package com.grupo3.BookVerse.features.friends;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.infrastructure.notifications.SocialMailRequested;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FriendshipService {
    private final FriendshipRepository friendships;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    public FriendshipService(FriendshipRepository friendships, UserRepository users, ApplicationEventPublisher events) { this.friendships = friendships; this.users = users; this.events = events; }

    private UserEntity current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserEntity user)) throw new AccessDeniedException("Authentication required");
        return user;
    }
    private UserEntity target(UUID id) {
        return users.findByIdExternalAndStatus(id, UserStatus.ACTIVE).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    private FriendshipDto dto(Friendship f, UserEntity me) {
        var other = Objects.equals(f.getUserLow().getId(), me.getId()) ? f.getUserHigh() : f.getUserLow();
        String state = f.getStatus() == Friendship.Status.ACCEPTED ? "FRIENDS" : Objects.equals(f.getRequester().getId(), me.getId()) ? "OUTGOING" : "INCOMING";
        return new FriendshipDto(f.getIdExternal(), other.getIdExternal(), other.getProfileUsername(), state, f.getCreatedAt());
    }
    @Transactional(readOnly = true)
    public FriendshipDto relationship(UUID userId) {
        var me = current(); var other = target(userId);
        if (Objects.equals(me.getId(), other.getId())) throw new BadRequestException("Cannot befriend yourself");
        long low = Math.min(me.getId(), other.getId()), high = Math.max(me.getId(), other.getId());
        return friendships.findPair(low, high).map(f -> dto(f, me)).orElse(new FriendshipDto(null, other.getIdExternal(), other.getProfileUsername(), "NONE", null));
    }
    @Transactional
    public FriendshipDto request(UUID userId) {
        var me = current(); var other = target(userId);
        if (Objects.equals(me.getId(), other.getId())) throw new BadRequestException("Cannot befriend yourself");
        long low = Math.min(me.getId(), other.getId()), high = Math.max(me.getId(), other.getId());
        // Lock both users in a consistent order so simultaneous reciprocal requests cannot create duplicates.
        var lower = users.lockActiveUser(low).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var higher = users.lockActiveUser(high).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (friendships.findPairForUpdate(low, high).isPresent()) throw new DuplicateResourceException("Friendship or request already exists");
        var f = new Friendship(); f.setIdExternal(UUID.randomUUID()); f.setUserLow(lower); f.setUserHigh(higher);
        f.setRequester(Objects.equals(me.getId(), low) ? lower : higher); f.setStatus(Friendship.Status.PENDING);
        f.setCreatedAt(LocalDateTime.now()); f.setUpdatedAt(f.getCreatedAt());
        var saved = friendships.save(f);
        events.publishEvent(new SocialMailRequested(other.getEmail(), other.getProfileUsername(), me.getProfileUsername(), null));
        return dto(saved, me);
    }
    @Transactional(readOnly = true)
    public List<FriendshipDto> pending() {
        var me = current();
        return friendships.findForUser(me.getId(), Friendship.Status.PENDING).stream().filter(f -> !Objects.equals(f.getRequester().getId(), me.getId())).map(f -> dto(f, me)).toList();
    }
    @Transactional(readOnly = true)
    public List<FriendshipDto> friends() {
        var me = current();
        return friendships.findForUser(me.getId(), Friendship.Status.ACCEPTED).stream().map(f -> dto(f, me)).toList();
    }
    @Transactional
    public void respond(UUID id, boolean accept) {
        var me = current();
        var f = friendships.findByExternalForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if ((!Objects.equals(f.getUserLow().getId(), me.getId()) && !Objects.equals(f.getUserHigh().getId(), me.getId())) || Objects.equals(f.getRequester().getId(), me.getId())) throw new AccessDeniedException("Only the recipient can respond");
        if (f.getStatus() != Friendship.Status.PENDING) throw new BadRequestException("Request is no longer pending");
        if (accept) { f.setStatus(Friendship.Status.ACCEPTED); f.setUpdatedAt(LocalDateTime.now()); }
        else friendships.delete(f);
    }
    @Transactional
    public void remove(UUID id) {
        var me = current();
        var f = friendships.findByExternalForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));
        if (!Objects.equals(f.getUserLow().getId(), me.getId()) && !Objects.equals(f.getUserHigh().getId(), me.getId())) throw new AccessDeniedException("Not a participant");
        if (f.getStatus() == Friendship.Status.PENDING && !Objects.equals(f.getRequester().getId(), me.getId())) throw new AccessDeniedException("Only the sender can cancel");
        friendships.delete(f);
    }
}
