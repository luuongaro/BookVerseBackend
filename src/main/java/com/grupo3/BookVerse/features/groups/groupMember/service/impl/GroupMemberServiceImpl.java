package com.grupo3.BookVerse.features.groups.groupMember.service.impl;

import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberRequestDto;
import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberResponseDto;
import com.grupo3.BookVerse.features.groups.groupMember.mappers.GroupMemberMapper;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupMember.service.GroupMemberService;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMemberServiceImpl implements GroupMemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupMemberMapper groupMemberMapper;
    private final ReadingGroupRepository readingGroupRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public GroupMemberResponseDto addMemberToGroup(
            UUID groupId,
            GroupMemberRequestDto requestDto
    ) {
        ReadingGroupEntity group = findGroupByIdExternal(groupId);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanManageMembers(groupId, authenticatedUser);

        String email = requestDto.getEmail().trim();
        UserEntity targetUser = findUserByEmail(email);

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Only active users can be added to a reading group"
            );
        }

        GroupMemberEntity existingMembership = groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(
                        groupId,
                        targetUser.getIdExternal()
                )
                .orElse(null);

        if (existingMembership != null) {
            if (existingMembership.getStatus() == GroupMemberStatus.ACTIVE) {
                throw new DuplicateResourceException(
                        "User is already an active member of this group"
                );
            }

            if (existingMembership.getStatus() == GroupMemberStatus.REMOVED) {
                throw new AccessDeniedException(
                        "A removed user cannot be added to this group again"
                );
            }

            existingMembership.setStatus(GroupMemberStatus.ACTIVE);
            existingMembership.setMemberType(GroupMemberType.MEMBER);
            existingMembership.setJoinedAt(LocalDateTime.now());

            GroupMemberEntity reactivatedMembership =
                    groupMemberRepository.save(existingMembership);

            return groupMemberMapper.toResponseDto(reactivatedMembership);
        }

        GroupMemberEntity groupMemberEntity = GroupMemberEntity.builder()
                .group(group)
                .user(targetUser)
                .memberType(GroupMemberType.MEMBER)
                .status(GroupMemberStatus.ACTIVE)
                .build();

        GroupMemberEntity savedGroupMember =
                groupMemberRepository.save(groupMemberEntity);

        return groupMemberMapper.toResponseDto(savedGroupMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponseDto> getActiveMembersByGroupId(UUID groupId) {
        findGroupByIdExternal(groupId);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanViewMembers(groupId, authenticatedUser);

        List<GroupMemberEntity> activeMembers =
                groupMemberRepository.findByGroupIdExternalAndStatus(groupId, GroupMemberStatus.ACTIVE);

        return groupMemberMapper.toResponseDtoList(activeMembers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponseDto> getActiveMembershipsByUserId(UUID userId) {
        UserEntity authenticatedUser = getAuthenticatedUser();
        findUserByIdExternal(userId);

        boolean isSameUser = Objects.equals(userId, authenticatedUser.getIdExternal());
        boolean isAdminOrModerator = isAdminOrModerator(authenticatedUser);

        if (!isSameUser && !isAdminOrModerator) {
            throw new AccessDeniedException("You do not have permission to view this user's memberships");
        }

        List<GroupMemberEntity> activeMemberships =
                groupMemberRepository.findByUserIdExternalAndStatus(userId, GroupMemberStatus.ACTIVE);

        return groupMemberMapper.toResponseDtoList(activeMemberships);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupMemberResponseDto getGroupMemberByIdExternal(UUID idExternal) {
        GroupMemberEntity groupMemberEntity = groupMemberRepository.findByIdExternal(idExternal)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group member not found with idExternal: " + idExternal
                ));

        UserEntity authenticatedUser = getAuthenticatedUser();

        boolean isSameUser = Objects.equals(
                groupMemberEntity.getUser().getIdExternal(),
                authenticatedUser.getIdExternal()
        );

        boolean isAdminOrModerator = isAdminOrModerator(authenticatedUser);

        boolean isActiveMemberOfSameGroup = groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndStatus(
                        groupMemberEntity.getGroup().getIdExternal(),
                        authenticatedUser.getIdExternal(),
                        GroupMemberStatus.ACTIVE
                );

        if (!isSameUser && !isAdminOrModerator && !isActiveMemberOfSameGroup) {
            throw new AccessDeniedException("You do not have permission to view this group membership");
        }

        return groupMemberMapper.toResponseDto(groupMemberEntity);
    }

    @Override
    @Transactional
    public void removeMemberFromGroup(UUID groupId, UUID userId) {
        findGroupByIdExternal(groupId);

        UserEntity authenticatedUser = getAuthenticatedUser();
        UserEntity targetUser = findUserByIdExternal(userId);

        validateCanManageMembers(groupId, authenticatedUser);

        GroupMemberEntity membership = groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(groupId, targetUser.getIdExternal())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group membership not found for user idExternal " + userId
                                + " in group idExternal " + groupId
                ));

        if (membership.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new AccessDeniedException("Only active memberships can be removed");
        }

        boolean targetIsCreator = membership.getMemberType() == GroupMemberType.CREATOR;
        boolean actorIsAdminOrModerator = isAdminOrModerator(authenticatedUser);

        if (targetIsCreator && !actorIsAdminOrModerator) {
            throw new AccessDeniedException("The group creator cannot be removed by a regular group manager");
        }

        membership.setStatus(GroupMemberStatus.REMOVED);
        groupMemberRepository.save(membership);
    }

    @Override
    @Transactional
    public void leaveGroup(UUID groupId) {
        findGroupByIdExternal(groupId);
        UserEntity authenticatedUser = getAuthenticatedUser();

        GroupMemberEntity membership = groupMemberRepository
                .findByGroupIdExternalAndUserIdExternal(groupId, authenticatedUser.getIdExternal())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group membership not found for user idExternal "
                                + authenticatedUser.getIdExternal() + " in group idExternal " + groupId
                ));

        if (membership.getStatus() != GroupMemberStatus.ACTIVE) {
            throw new AccessDeniedException("Only active memberships can leave a group");
        }

        if (membership.getMemberType() == GroupMemberType.CREATOR) {
            throw new AccessDeniedException("The group creator cannot leave the group");
        }

        membership.setStatus(GroupMemberStatus.LEFT);
        groupMemberRepository.save(membership);
    }

    private void validateCanManageMembers(UUID groupId, UserEntity authenticatedUser) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isCreator = groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                        groupId,
                        authenticatedUser.getIdExternal(),
                        GroupMemberType.CREATOR,
                        GroupMemberStatus.ACTIVE
                );

        if (!isCreator) {
            throw new AccessDeniedException(
                    "Only the group creator, an admin, or a moderator can manage members"
            );
        }
    }

    private void validateCanViewMembers(UUID groupId, UserEntity authenticatedUser) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isActiveMember = groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndStatus(
                        groupId,
                        authenticatedUser.getIdExternal(),
                        GroupMemberStatus.ACTIVE
                );

        if (!isActiveMember) {
            throw new AccessDeniedException("Only active group members can view group members");
        }
    }

    private boolean isAdminOrModerator(UserEntity user) {
        return user.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_MODERATOR"));
    }

    private ReadingGroupEntity findGroupByIdExternal(UUID groupId) {
        return readingGroupRepository.findByIdExternal(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group not found with idExternal: " + groupId
                ));
    }

    private UserEntity findUserByIdExternal(UUID userId) {
        return userRepository.findByIdExternal(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with idExternal: " + userId
                ));
    }

    private UserEntity findUserByEmail(String email) {
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email
                ));
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserEntity authenticatedUser)) {
            throw new AccessDeniedException("Authenticated principal is not a valid user");
        }

        return authenticatedUser;
    }

}
