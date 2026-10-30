package com.grupo3.BookVerse.features.users;

import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.features.groups.groupMember.domain.*;
import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberRequestDto;
import com.grupo3.BookVerse.features.groups.groupMember.mappers.GroupMemberMapper;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupMember.service.impl.GroupMemberServiceImpl;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityGroupMemberTest {
    @Mock GroupMemberRepository members;
    @Mock GroupMemberMapper mapper;
    @Mock ReadingGroupRepository groups;
    @Mock UserRepository users;
    @InjectMocks GroupMemberServiceImpl service;
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UserEntity actor;
    UserEntity target;

    @BeforeEach void setup() {
        actor = new UserEntity();
        actor.setIdExternal(UUID.randomUUID());
        target = new UserEntity();
        target.setIdExternal(userId);
        target.setStatus(UserStatus.ACTIVE);
        var group = new ReadingGroupEntity();
        group.setIdExternal(groupId);
        when(groups.findByIdExternal(groupId)).thenReturn(Optional.of(group));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, actor.getAuthorities()));
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private void creator() {
        when(members.existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                groupId, actor.getIdExternal(), GroupMemberType.CREATOR, GroupMemberStatus.ACTIVE)).thenReturn(true);
    }

    @Test void addsSelectedUserByIdWithoutLookingUpOrRequiringEmail() {
        creator();
        when(users.findByIdExternal(userId)).thenReturn(Optional.of(target));
        service.addMemberToGroup(groupId, GroupMemberRequestDto.builder().userId(userId).build());
        var saved = ArgumentCaptor.forClass(GroupMemberEntity.class);
        verify(members).save(saved.capture());
        assertEquals(target, saved.getValue().getUser());
        assertEquals(GroupMemberType.MEMBER, saved.getValue().getMemberType());
        assertEquals(GroupMemberStatus.ACTIVE, saved.getValue().getStatus());
        verify(users, never()).findByEmailWithRoles(anyString());
    }

    @Test void retainsEmailCompatibility() {
        creator();
        when(users.findByEmailWithRoles("reader@example.test")).thenReturn(Optional.of(target));
        service.addMemberToGroup(groupId, new GroupMemberRequestDto("reader@example.test"));
        verify(members).save(any(GroupMemberEntity.class));
        verify(users, never()).findByIdExternal(any());
    }

    @Test void regularMembersCannotAddSomeoneOrLookUpTheirPrivateData() {
        assertThrows(AccessDeniedException.class,
                () -> service.addMemberToGroup(groupId, GroupMemberRequestDto.builder().userId(userId).build()));
        verifyNoInteractions(users);
        verify(members, never()).save(any());
    }

    @Test void inactiveTargetsAreRejected() {
        creator();
        target.setStatus(UserStatus.INACTIVE);
        when(users.findByIdExternal(userId)).thenReturn(Optional.of(target));
        assertThrows(AccessDeniedException.class,
                () -> service.addMemberToGroup(groupId, GroupMemberRequestDto.builder().userId(userId).build()));
        verify(members, never()).save(any());
    }

    @Test void activeMembershipsCannotBeAddedTwice() {
        creator();
        when(users.findByIdExternal(userId)).thenReturn(Optional.of(target));
        var existing = GroupMemberEntity.builder().status(GroupMemberStatus.ACTIVE).build();
        when(members.findByGroupIdExternalAndUserIdExternal(groupId, userId)).thenReturn(Optional.of(existing));
        assertThrows(DuplicateResourceException.class,
                () -> service.addMemberToGroup(groupId, GroupMemberRequestDto.builder().userId(userId).build()));
        verify(members, never()).save(any());
    }

    @Test void removedMembershipsRemainBlocked() {
        creator();
        when(users.findByIdExternal(userId)).thenReturn(Optional.of(target));
        var existing = GroupMemberEntity.builder().status(GroupMemberStatus.REMOVED).build();
        when(members.findByGroupIdExternalAndUserIdExternal(groupId, userId)).thenReturn(Optional.of(existing));
        assertThrows(AccessDeniedException.class,
                () -> service.addMemberToGroup(groupId, GroupMemberRequestDto.builder().userId(userId).build()));
        verify(members, never()).save(any());
    }

    @Test void aUserWhoLeftCanBeAddedAgain() {
        creator();
        when(users.findByIdExternal(userId)).thenReturn(Optional.of(target));
        var existing = GroupMemberEntity.builder().status(GroupMemberStatus.LEFT).build();
        when(members.findByGroupIdExternalAndUserIdExternal(groupId, userId)).thenReturn(Optional.of(existing));
        service.addMemberToGroup(groupId, GroupMemberRequestDto.builder().userId(userId).build());
        assertEquals(GroupMemberStatus.ACTIVE, existing.getStatus());
        assertNotNull(existing.getJoinedAt());
        verify(members).save(existing);
    }
}
