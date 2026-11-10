package com.grupo3.BookVerse.features.groups.groupMember.service.impl;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberResponseDto;
import com.grupo3.BookVerse.features.groups.groupMember.mappers.GroupMemberMapper;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupMemberServiceImplTest {

    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private GroupMemberMapper groupMemberMapper;
    @Mock private ReadingGroupRepository readingGroupRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GroupMemberServiceImpl service;

    private UUID groupId;
    private UUID userId;
    private UUID membershipId;

    private UserEntity authenticatedUser;
    private ReadingGroupEntity group;
    private UserEntity targetUser;

    @BeforeEach
    void setUp() {
        groupId      = UUID.randomUUID();
        userId       = UUID.randomUUID();
        membershipId = UUID.randomUUID();

        authenticatedUser = new UserEntity();
        authenticatedUser.setIdExternal(UUID.randomUUID());

        targetUser = new UserEntity();
        targetUser.setIdExternal(userId);
        targetUser.setStatus(UserStatus.ACTIVE);

        group = new ReadingGroupEntity();
        group.setIdExternal(groupId);
        group.setIsActive(true);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser, null, Collections.emptyList()
                );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }


    // ─── getActiveMembersByGroupId ───────────────────────────────────────────

    // Verify active members list is returned for active member
    @Test
    void getActiveMembersByGroupId_shouldReturnList_whenUserIsActiveMember() {
        List<GroupMemberEntity> members = List.of(new GroupMemberEntity());
        List<GroupMemberResponseDto> responses = List.of(mock(GroupMemberResponseDto.class));

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndStatus(
                        groupId, authenticatedUser.getIdExternal(), GroupMemberStatus.ACTIVE))
                .thenReturn(true);
        when(groupMemberRepository
                .findByGroupIdExternalAndStatus(groupId, GroupMemberStatus.ACTIVE))
                .thenReturn(members);
        when(groupMemberMapper.toResponseDtoList(members)).thenReturn(responses);

        List<GroupMemberResponseDto> result = service.getActiveMembersByGroupId(groupId);

        assertEquals(responses, result);
    }

    // Verify exception when non-member tries to view members
    @Test
    void getActiveMembersByGroupId_shouldThrowAccessDenied_whenNotMember() {
        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndStatus(
                        groupId, authenticatedUser.getIdExternal(), GroupMemberStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.getActiveMembersByGroupId(groupId));
    }

    // ─── getActiveMembershipsByUserId ────────────────────────────────────────

    // Verify user can view their own memberships
    @Test
    void getActiveMembershipsByUserId_shouldReturnList_whenSameUser() {
        UUID selfId = authenticatedUser.getIdExternal();

        List<GroupMemberEntity> memberships = List.of(new GroupMemberEntity());
        List<GroupMemberResponseDto> responses = List.of(mock(GroupMemberResponseDto.class));

        UserEntity self = new UserEntity();
        self.setIdExternal(selfId);

        when(userRepository.findByIdExternal(selfId)).thenReturn(Optional.of(self));
        when(groupMemberRepository
                .findByUserIdExternalAndStatus(selfId, GroupMemberStatus.ACTIVE))
                .thenReturn(memberships);
        when(groupMemberMapper.toResponseDtoList(memberships)).thenReturn(responses);

        List<GroupMemberResponseDto> result = service.getActiveMembershipsByUserId(selfId);

        assertEquals(responses, result);
    }

    // Verify exception when user tries to view another user's memberships
    @Test
    void getActiveMembershipsByUserId_shouldThrowAccessDenied_whenDifferentUser() {
        UUID otherId = UUID.randomUUID();
        UserEntity other = new UserEntity();
        other.setIdExternal(otherId);

        when(userRepository.findByIdExternal(otherId)).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class,
                () -> service.getActiveMembershipsByUserId(otherId));
    }

    // ─── getGroupMemberByIdExternal ──────────────────────────────────────────

    // Verify membership is returned when user is the owner
    @Test
    void getGroupMemberByIdExternal_shouldReturnDto_whenSameUser() {
        GroupMemberEntity membership = new GroupMemberEntity();
        membership.setUser(authenticatedUser);

        ReadingGroupEntity memberGroup = new ReadingGroupEntity();
        memberGroup.setIdExternal(groupId);
        membership.setGroup(memberGroup);

        GroupMemberResponseDto response = mock(GroupMemberResponseDto.class);

        when(groupMemberRepository.findByIdExternal(membershipId))
                .thenReturn(Optional.of(membership));
        when(groupMemberMapper.toResponseDto(membership)).thenReturn(response);

        GroupMemberResponseDto result = service.getGroupMemberByIdExternal(membershipId);

        assertEquals(response, result);
    }

    // Verify exception when membership is not found
    @Test
    void getGroupMemberByIdExternal_shouldThrowException_whenNotFound() {
        when(groupMemberRepository.findByIdExternal(membershipId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getGroupMemberByIdExternal(membershipId));
    }

    // ─── removeMemberFromGroup ───────────────────────────────────────────────

    // Verify a regular member is removed successfully by the creator
    @Test
    void removeMemberFromGroup_shouldRemoveMember_whenCalledByCreator() {
        GroupMemberEntity membership = new GroupMemberEntity();
        membership.setStatus(GroupMemberStatus.ACTIVE);
        membership.setMemberType(GroupMemberType.MEMBER);

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(targetUser));
        when(groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                        groupId, authenticatedUser.getIdExternal(),
                        GroupMemberType.CREATOR, GroupMemberStatus.ACTIVE))
                .thenReturn(true);
        when(groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(groupId, userId))
                .thenReturn(Optional.of(membership));

        service.removeMemberFromGroup(groupId, userId);

        assertEquals(GroupMemberStatus.REMOVED, membership.getStatus());
        verify(groupMemberRepository).save(membership);
    }

    // Verify exception when trying to remove an inactive member
    @Test
    void removeMemberFromGroup_shouldThrowAccessDenied_whenMemberNotActive() {
        GroupMemberEntity membership = new GroupMemberEntity();
        membership.setStatus(GroupMemberStatus.LEFT);
        membership.setMemberType(GroupMemberType.MEMBER);

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(targetUser));
        when(groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                        groupId, authenticatedUser.getIdExternal(),
                        GroupMemberType.CREATOR, GroupMemberStatus.ACTIVE))
                .thenReturn(true);
        when(groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(groupId, userId))
                .thenReturn(Optional.of(membership));

        assertThrows(AccessDeniedException.class,
                () -> service.removeMemberFromGroup(groupId, userId));
    }

    // Verify exception when non-creator/non-admin tries to remove a member
    @Test
    void removeMemberFromGroup_shouldThrowAccessDenied_whenNotCreatorOrAdmin() {
        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(targetUser));
        when(groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                        groupId, authenticatedUser.getIdExternal(),
                        GroupMemberType.CREATOR, GroupMemberStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> service.removeMemberFromGroup(groupId, userId));
    }

    // ─── leaveGroup ──────────────────────────────────────────────────────────

    // Verify a regular member can leave a group
    @Test
    void leaveGroup_shouldSetStatusLeft_whenActiveMember() {
        GroupMemberEntity membership = new GroupMemberEntity();
        membership.setStatus(GroupMemberStatus.ACTIVE);
        membership.setMemberType(GroupMemberType.MEMBER);

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(
                        groupId, authenticatedUser.getIdExternal()))
                .thenReturn(Optional.of(membership));

        service.leaveGroup(groupId);

        assertEquals(GroupMemberStatus.LEFT, membership.getStatus());
        verify(groupMemberRepository).save(membership);
    }

    // Verify ownership is transferred when creator leaves and there are other active members
    @Test
    void leaveGroup_shouldTransferOwnership_whenCreatorLeaves() {
        GroupMemberEntity creatorMembership = new GroupMemberEntity();
        creatorMembership.setStatus(GroupMemberStatus.ACTIVE);
        creatorMembership.setMemberType(GroupMemberType.CREATOR);
        creatorMembership.setUser(authenticatedUser);

        UserEntity otherUser = new UserEntity();
        otherUser.setIdExternal(UUID.randomUUID());
        otherUser.setStatus(UserStatus.ACTIVE);

        GroupMemberEntity nextCreator = new GroupMemberEntity();
        nextCreator.setStatus(GroupMemberStatus.ACTIVE);
        nextCreator.setMemberType(GroupMemberType.MEMBER);
        nextCreator.setUser(otherUser);
        nextCreator.setJoinedAt(java.time.LocalDateTime.now().minusDays(1));

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(
                        groupId, authenticatedUser.getIdExternal()))
                .thenReturn(Optional.of(creatorMembership));
        when(groupMemberRepository
                .findByGroupIdExternalAndStatus(groupId, GroupMemberStatus.ACTIVE))
                .thenReturn(List.of(creatorMembership, nextCreator));

        service.leaveGroup(groupId);

        assertEquals(GroupMemberStatus.LEFT, creatorMembership.getStatus());
        assertEquals(GroupMemberType.CREATOR, nextCreator.getMemberType());
        assertEquals(otherUser, group.getCreatedBy());
        verify(groupMemberRepository).save(nextCreator);
        verify(readingGroupRepository).save(group);
        verify(groupMemberRepository).save(creatorMembership);
    }

    // Verify exception when membership is not found on leave
    @Test
    void leaveGroup_shouldThrowException_whenMembershipNotFound() {
        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(
                        groupId, authenticatedUser.getIdExternal()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.leaveGroup(groupId));
    }

    // Verify group is deactivated when creator leaves as the only active member
    @Test
    void leaveGroup_shouldDeactivateGroup_whenCreatorIsOnlyActiveMember() {
        GroupMemberEntity creatorMembership = new GroupMemberEntity();
        creatorMembership.setStatus(GroupMemberStatus.ACTIVE);
        creatorMembership.setMemberType(GroupMemberType.CREATOR);
        creatorMembership.setUser(authenticatedUser);

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));
        when(groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(
                        groupId, authenticatedUser.getIdExternal()))
                .thenReturn(Optional.of(creatorMembership));
        when(groupMemberRepository
                .findByGroupIdExternalAndStatus(groupId, GroupMemberStatus.ACTIVE))
                .thenReturn(List.of(creatorMembership));

        service.leaveGroup(groupId);

        assertFalse(group.getIsActive());
        assertEquals(GroupMemberStatus.LEFT, creatorMembership.getStatus());
        verify(readingGroupRepository).save(group);
        verify(groupMemberRepository).save(creatorMembership);
    }
}
