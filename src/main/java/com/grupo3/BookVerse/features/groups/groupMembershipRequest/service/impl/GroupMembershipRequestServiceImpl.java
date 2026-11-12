package com.grupo3.BookVerse.features.groups.groupMembershipRequest.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestEntity;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestStatus;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestType;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto.GroupMembershipInvitationRequestDto;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto.GroupMembershipRequestResponseDto;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.mappers.GroupMembershipRequestMapper;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.repository.GroupMembershipRequestRepository;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.service.GroupMembershipRequestService;
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
import org.springframework.context.ApplicationEventPublisher;
import com.grupo3.BookVerse.infrastructure.notifications.SocialMailRequested;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupMembershipRequestServiceImpl
        implements GroupMembershipRequestService {

    private final GroupMembershipRequestRepository requestRepository;
    private final GroupMembershipRequestMapper requestMapper;
    private final ReadingGroupRepository readingGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public GroupMembershipRequestResponseDto requestToJoin(
            UUID groupId
    ) {
        ReadingGroupEntity group =
                findActiveGroup(groupId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateActiveUser(authenticatedUser);

        validateCanBecomeMember(
                groupId,
                authenticatedUser
        );

        validateNoPendingMembershipProcess(
                groupId,
                authenticatedUser.getIdExternal()
        );

        GroupMembershipRequestEntity request =
                GroupMembershipRequestEntity.builder()
                        .group(group)
                        .targetUser(authenticatedUser)
                        .createdBy(authenticatedUser)
                        .type(
                                GroupMembershipRequestType.JOIN_REQUEST
                        )
                        .status(
                                GroupMembershipRequestStatus.PENDING
                        )
                        .build();

        GroupMembershipRequestEntity saved =
                requestRepository.save(request);

        UserEntity creator = group.getCreatedBy();
        events.publishEvent(new SocialMailRequested(creator.getEmail(), creator.getProfileUsername(), authenticatedUser.getProfileUsername(), group.getName()));

        return requestMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public GroupMembershipRequestResponseDto inviteUser(
            UUID groupId,
            GroupMembershipInvitationRequestDto requestDto
    ) {
        if (requestDto == null
                || requestDto.targetUserId() == null) {
            throw new BadRequestException(
                    "Target user id cannot be null"
            );
        }

        ReadingGroupEntity group =
                findActiveGroup(groupId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateActiveUser(authenticatedUser);

        validateCanInvite(
                groupId,
                authenticatedUser
        );

        UserEntity targetUser =
                findUserByIdExternal(
                        requestDto.targetUserId()
                );

        validateActiveUser(targetUser);

        validateCanBecomeMember(
                groupId,
                targetUser
        );

        validateNoPendingMembershipProcess(
                groupId,
                targetUser.getIdExternal()
        );

        GroupMembershipRequestEntity invitation =
                GroupMembershipRequestEntity.builder()
                        .group(group)
                        .targetUser(targetUser)
                        .createdBy(authenticatedUser)
                        .type(
                                GroupMembershipRequestType.INVITATION
                        )
                        .status(
                                GroupMembershipRequestStatus.PENDING
                        )
                        .build();

        GroupMembershipRequestEntity saved =
                requestRepository.save(invitation);

        return requestMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public GroupMembershipRequestResponseDto acceptRequest(
            UUID requestId
    ) {
        GroupMembershipRequestEntity request =
                findPendingRequest(requestId);

        ReadingGroupEntity group =
                request.getGroup();

        validateGroupIsActive(group);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateActiveUser(authenticatedUser);

        validateCanRespond(
                request,
                authenticatedUser
        );

        UserEntity targetUser =
                request.getTargetUser();

        validateActiveUser(targetUser);

        activateMembership(
                group,
                targetUser
        );

        request.setStatus(
                GroupMembershipRequestStatus.ACCEPTED
        );

        request.setRespondedAt(
                LocalDateTime.now()
        );

        GroupMembershipRequestEntity saved =
                requestRepository.save(request);

        return requestMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public GroupMembershipRequestResponseDto rejectRequest(
            UUID requestId
    ) {
        GroupMembershipRequestEntity request =
                findPendingRequest(requestId);

        validateGroupIsActive(
                request.getGroup()
        );

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateActiveUser(authenticatedUser);

        validateCanRespond(
                request,
                authenticatedUser
        );

        request.setStatus(
                GroupMembershipRequestStatus.REJECTED
        );

        request.setRespondedAt(
                LocalDateTime.now()
        );

        GroupMembershipRequestEntity saved =
                requestRepository.save(request);

        return requestMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public GroupMembershipRequestResponseDto cancelRequest(
            UUID requestId
    ) {
        GroupMembershipRequestEntity request =
                findPendingRequest(requestId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        boolean createdByAuthenticatedUser =
                request.getCreatedBy() != null
                        && Objects.equals(
                        request.getCreatedBy()
                                .getIdExternal(),
                        authenticatedUser.getIdExternal()
                );

        if (!createdByAuthenticatedUser) {
            throw new AccessDeniedException(
                    "Only the user who created the membership request can cancel it"
            );
        }

        request.setStatus(
                GroupMembershipRequestStatus.CANCELLED
        );

        request.setRespondedAt(
                LocalDateTime.now()
        );

        GroupMembershipRequestEntity saved =
                requestRepository.save(request);

        return requestMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMembershipRequestResponseDto>
    getPendingJoinRequests(
            UUID groupId
    ) {
        findActiveGroup(groupId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanManageJoinRequests(
                groupId,
                authenticatedUser
        );

        List<GroupMembershipRequestEntity> requests =
                requestRepository
                        .findByGroup_IdExternalAndTypeAndStatusOrderByCreatedAtAsc(
                                groupId,
                                GroupMembershipRequestType.JOIN_REQUEST,
                                GroupMembershipRequestStatus.PENDING
                        );

        return requestMapper.toResponseDtoList(
                requests
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMembershipRequestResponseDto>
    getMyPendingInvitations() {
        UserEntity authenticatedUser =
                getAuthenticatedUser();

        List<GroupMembershipRequestEntity> invitations =
                requestRepository
                        .findByTargetUser_IdExternalAndTypeAndStatusOrderByCreatedAtDesc(
                                authenticatedUser.getIdExternal(),
                                GroupMembershipRequestType.INVITATION,
                                GroupMembershipRequestStatus.PENDING
                        );

        return requestMapper.toResponseDtoList(
                invitations
        );
    }

    private void activateMembership(
            ReadingGroupEntity group,
            UserEntity targetUser
    ) {
        GroupMemberEntity existingMembership =
                groupMemberRepository
                        .findByGroupIdExternalAndUserIdExternal(
                                group.getIdExternal(),
                                targetUser.getIdExternal()
                        )
                        .orElse(null);

        if (existingMembership != null) {

            if (existingMembership.getStatus()
                    == GroupMemberStatus.ACTIVE) {
                throw new DuplicateResourceException(
                        "User is already an active member of this group"
                );
            }

            if (existingMembership.getStatus()
                    == GroupMemberStatus.REMOVED) {
                throw new AccessDeniedException(
                        "A removed user cannot rejoin this group"
                );
            }

            existingMembership.setStatus(
                    GroupMemberStatus.ACTIVE
            );

            existingMembership.setMemberType(
                    GroupMemberType.MEMBER
            );

            existingMembership.setJoinedAt(
                    LocalDateTime.now()
            );

            groupMemberRepository.save(
                    existingMembership
            );

            return;
        }

        GroupMemberEntity membership =
                GroupMemberEntity.builder()
                        .group(group)
                        .user(targetUser)
                        .memberType(
                                GroupMemberType.MEMBER
                        )
                        .status(
                                GroupMemberStatus.ACTIVE
                        )
                        .build();

        groupMemberRepository.save(membership);
    }

    private void validateCanRespond(
            GroupMembershipRequestEntity request,
            UserEntity authenticatedUser
    ) {
        if (request.getType()
                == GroupMembershipRequestType.JOIN_REQUEST) {

            validateCanManageJoinRequests(
                    request.getGroup().getIdExternal(),
                    authenticatedUser
            );

            return;
        }

        if (request.getType()
                == GroupMembershipRequestType.INVITATION) {

            boolean isTargetUser =
                    request.getTargetUser() != null
                            && Objects.equals(
                            request.getTargetUser()
                                    .getIdExternal(),
                            authenticatedUser
                                    .getIdExternal()
                    );

            if (!isTargetUser) {
                throw new AccessDeniedException(
                        "Only the invited user can accept or reject this invitation"
                );
            }

            return;
        }

        throw new BadRequestException(
                "Unsupported membership request type"
        );
    }

    private void validateCanManageJoinRequests(
            UUID groupId,
            UserEntity authenticatedUser
    ) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isCreator =
                groupMemberRepository
                        .existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                                groupId,
                                authenticatedUser.getIdExternal(),
                                GroupMemberType.CREATOR,
                                GroupMemberStatus.ACTIVE
                        );

        if (!isCreator) {
            throw new AccessDeniedException(
                    "Only the group creator, an admin, or a moderator can manage join requests"
            );
        }
    }

    private void validateCanInvite(
            UUID groupId,
            UserEntity authenticatedUser
    ) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isActiveMember =
                groupMemberRepository
                        .existsByGroupIdExternalAndUserIdExternalAndStatus(
                                groupId,
                                authenticatedUser.getIdExternal(),
                                GroupMemberStatus.ACTIVE
                        );

        if (!isActiveMember) {
            throw new AccessDeniedException(
                    "Only active group members can invite users"
            );
        }
    }

    private void validateCanBecomeMember(
            UUID groupId,
            UserEntity targetUser
    ) {
        GroupMemberEntity membership =
                groupMemberRepository
                        .findByGroupIdExternalAndUserIdExternal(
                                groupId,
                                targetUser.getIdExternal()
                        )
                        .orElse(null);

        if (membership == null) {
            return;
        }

        if (membership.getStatus()
                == GroupMemberStatus.ACTIVE) {
            throw new DuplicateResourceException(
                    "User is already an active member of this group"
            );
        }

        if (membership.getStatus()
                == GroupMemberStatus.REMOVED) {
            throw new AccessDeniedException(
                    "A removed user cannot rejoin this group"
            );
        }

        // LEFT puede crear una nueva solicitud o recibir
        // una nueva invitación. La membresía se reactiva
        // únicamente cuando el proceso sea aceptado.
    }

    private void validateNoPendingMembershipProcess(
            UUID groupId,
            UUID targetUserId
    ) {
        boolean pendingJoinRequest =
                requestRepository
                        .existsByGroup_IdExternalAndTargetUser_IdExternalAndTypeAndStatus(
                                groupId,
                                targetUserId,
                                GroupMembershipRequestType.JOIN_REQUEST,
                                GroupMembershipRequestStatus.PENDING
                        );

        boolean pendingInvitation =
                requestRepository
                        .existsByGroup_IdExternalAndTargetUser_IdExternalAndTypeAndStatus(
                                groupId,
                                targetUserId,
                                GroupMembershipRequestType.INVITATION,
                                GroupMembershipRequestStatus.PENDING
                        );

        if (pendingJoinRequest || pendingInvitation) {
            throw new DuplicateResourceException(
                    "A pending membership process already exists for this user and group"
            );
        }
    }

    private GroupMembershipRequestEntity findPendingRequest(
            UUID requestId
    ) {
        GroupMembershipRequestEntity request =
                requestRepository
                        .findByIdExternal(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Group membership request not found with idExternal: "
                                                + requestId
                                )
                        );

        if (request.getStatus()
                != GroupMembershipRequestStatus.PENDING) {
            throw new BadRequestException(
                    "Only pending membership requests can be processed"
            );
        }

        return request;
    }

    private ReadingGroupEntity findActiveGroup(
            UUID groupId
    ) {
        ReadingGroupEntity group =
                readingGroupRepository
                        .findByIdExternal(groupId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reading group not found with idExternal: "
                                                + groupId
                                )
                        );

        validateGroupIsActive(group);

        return group;
    }

    private void validateGroupIsActive(
            ReadingGroupEntity group
    ) {
        if (group == null
                || !Boolean.TRUE.equals(
                group.getIsActive()
        )) {
            throw new BadRequestException(
                    "Inactive reading groups cannot manage membership requests"
            );
        }
    }

    private UserEntity findUserByIdExternal(
            UUID userId
    ) {
        return userRepository
                .findByIdExternal(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with idExternal: "
                                        + userId
                        )
                );
    }

    private void validateActiveUser(
            UserEntity user
    ) {
        if (user == null
                || user.getStatus()
                != UserStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Only active users can participate in group membership requests"
            );
        }
    }

    private boolean isAdminOrModerator(
            UserEntity user
    ) {
        return user.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(
                                authority.getAuthority()
                        )
                                || "ROLE_MODERATOR".equals(
                                authority.getAuthority()
                        )
                );
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof UserEntity user)) {
            throw new AccessDeniedException(
                    "Authenticated user not found"
            );
        }

        return user;
    }
}
