package com.grupo3.BookVerse.features.groups.readingGroups.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestStatus;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestType;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.repository.GroupMembershipRequestRepository;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.mappers.GroupReadingCycleMapper;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.GroupMembershipState;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupDiscoveryResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.UpdateReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.mappers.ReadingGroupMapper;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.service.ReadingGroupService;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReadingGroupServiceImpl implements ReadingGroupService {

    private final ReadingGroupRepository repository;
    private final ReadingGroupMapper mapper;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupReadingCycleRepository groupReadingCycleRepository;
    private final GroupReadingCycleMapper groupReadingCycleMapper;
    private final GroupMembershipRequestRepository membershipRequestRepository;

    @Override
    @Transactional
    public ReadingGroupResponseDto createGroup(
            ReadingGroupRequestDto dto
    ) {
        UserEntity authenticatedUser =
                getAuthenticatedUser();

        String normalizedName =
                normalizeName(dto.name());

        validateUniqueActiveGroupName(
                normalizedName
        );

        ReadingGroupEntity entity =
                mapper.toEntity(dto);

        entity.setName(normalizedName);
        entity.setCreatedBy(authenticatedUser);
        entity.setIsActive(Boolean.TRUE);

        ReadingGroupEntity saved =
                repository.save(entity);

        GroupMemberEntity creatorMembership =
                GroupMemberEntity.builder()
                        .group(saved)
                        .user(authenticatedUser)
                        .memberType(GroupMemberType.CREATOR)
                        .status(GroupMemberStatus.ACTIVE)
                        .build();

        groupMemberRepository.save(
                creatorMembership
        );

        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupResponseDto> getAllGroups() {
        return mapper.toResponseDtoList(
                repository.findByIsActiveTrue()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReadingGroupResponseDto getGroupByIdExternal(
            UUID idExternal
    ) {
        ReadingGroupEntity entity =
                findGroupByIdExternal(idExternal);

        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new ResourceNotFoundException(
                    "Reading group not found with idExternal: "
                            + idExternal
            );
        }

        return mapper.toResponseDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupResponseDto>
    getGroupsByBookIdExternal(UUID bookId) {

        List<ReadingGroupEntity> groups =
                repository
                        .findDistinctByReadingCycles_Book_IdExternalAndIsActiveTrue(
                                bookId
                        );

        return mapper.toResponseDtoList(groups);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupResponseDto>
    getGroupsByStoryIdExternal(UUID storyId) {

        List<ReadingGroupEntity> groups =
                repository
                        .findDistinctByReadingCycles_Story_IdExternalAndIsActiveTrue(
                                storyId
                        );

        return mapper.toResponseDtoList(groups);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupResponseDto>
    getGroupsByUserIdExternal(UUID userId) {

        findUserByIdExternal(userId);

        return mapper.toResponseDtoList(
                repository
                        .findByCreatedBy_IdExternalAndIsActiveTrue(
                                userId
                        )
        );
    }

    @Override
    @Transactional
    public ReadingGroupResponseDto updateGroup(
            UUID idExternal,
            UpdateReadingGroupRequestDto dto
    ) {
        ReadingGroupEntity entity =
                findGroupByIdExternal(idExternal);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        String normalizedName =
                normalizeName(dto.name());

        validateCanManageGroup(
                entity,
                authenticatedUser
        );

        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new BadRequestException(
                    "Inactive reading groups cannot be updated"
            );
        }

        validateUniqueActiveGroupNameForUpdate(
                entity,
                normalizedName
        );

        entity.setName(normalizedName);

        ReadingGroupEntity updated =
                repository.save(entity);

        return mapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteGroup(UUID idExternal) {

        ReadingGroupEntity entity =
                findGroupByIdExternal(idExternal);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanManageGroup(
                entity,
                authenticatedUser
        );

        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new BadRequestException(
                    "Reading group is already inactive"
            );
        }

        entity.setIsActive(Boolean.FALSE);

        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupDiscoveryResponseDto> discoverGroups() {

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        return buildDiscoveryResponses(
                repository.findByIsActiveTrue(),
                authenticatedUser
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupDiscoveryResponseDto> searchGroups(
            String query
    ) {
        UserEntity authenticatedUser =
                getAuthenticatedUser();

        if (query == null || query.isBlank()) {

            return buildDiscoveryResponses(
                    repository.findByIsActiveTrue(),
                    authenticatedUser
            );
        }

        String normalizedQuery =
                query.trim();

        List<ReadingGroupEntity> groups =
                repository
                        .findByNameContainingIgnoreCaseAndIsActiveTrue(
                                normalizedQuery
                        );

        return buildDiscoveryResponses(
                groups,
                authenticatedUser
        );
    }

    private List<ReadingGroupDiscoveryResponseDto> buildDiscoveryResponses(
            List<ReadingGroupEntity> groups,
            UserEntity authenticatedUser
    ) {

        UUID userId =
                authenticatedUser.getIdExternal();

        return groups.stream()
                .map(group -> {

                    UUID groupId =
                            group.getIdExternal();

                    long activeMembersCount =
                            groupMemberRepository
                                    .countByGroupIdExternalAndStatus(
                                            groupId,
                                            GroupMemberStatus.ACTIVE
                                    );

                    GroupReadingCycleEntity activeCycle =
                            groupReadingCycleRepository
                                    .findByGroup_IdExternalAndStatus(
                                            groupId,
                                            GroupReadingCycleStatus.ACTIVE
                                    )
                                    .orElse(null);

                    GroupMembershipState membershipState =
                            resolveMembershipState(
                                    groupId,
                                    userId
                            );

                    String activeContentTitle = null;
                    String activeContentThumbnailUrl = null;

                    if (activeCycle != null) {

                        if (activeCycle.getBook() != null) {

                            activeContentTitle =
                                    activeCycle
                                            .getBook()
                                            .getTitle();

                            activeContentThumbnailUrl =
                                    activeCycle
                                            .getBook()
                                            .getThumbnailUrl();

                        } else if (activeCycle.getStory() != null) {

                            activeContentTitle =
                                    activeCycle
                                            .getStory()
                                            .getTitle();
                        }
                    }

                    return new ReadingGroupDiscoveryResponseDto(
                            groupId,
                            group.getName(),
                            Math.toIntExact(activeMembersCount),
                            activeCycle == null
                                    ? null
                                    : groupReadingCycleMapper
                                    .toResponseDto(activeCycle),
                            activeContentTitle,
                            activeContentThumbnailUrl,
                            membershipState
                    );
                })
                .toList();
    }

    private void validateUniqueActiveGroupName(
            String name
    ) {
        boolean exists =
                repository
                        .existsByNameIgnoreCaseAndIsActiveTrue(
                                name
                        );

        if (exists) {
            throw new BadRequestException(
                    "An active reading group with this name already exists"
            );
        }
    }

    private void validateUniqueActiveGroupNameForUpdate(
            ReadingGroupEntity entity,
            String name
    ) {
        boolean exists =
                repository
                        .existsByNameIgnoreCaseAndIsActiveTrueAndIdExternalNot(
                                name,
                                entity.getIdExternal()
                        );

        if (exists) {
            throw new BadRequestException(
                    "An active reading group with this name already exists"
            );
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private void validateCanManageGroup(
            ReadingGroupEntity entity,
            UserEntity authenticatedUser
    ) {
        boolean isOwner =
                entity.getCreatedBy() != null
                        && Objects.equals(
                        entity.getCreatedBy().getId(),
                        authenticatedUser.getId()
                );

        if (!isOwner
                && !isAdminOrModerator(authenticatedUser)) {

            throw new AccessDeniedException(
                    "You do not have permission to manage this reading group"
            );
        }
    }

    private boolean isAdminOrModerator(
            UserEntity user
    ) {
        return user.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_ADMIN")
                                || authority.getAuthority()
                                .equals("ROLE_MODERATOR")
                );
    }

    private ReadingGroupEntity findGroupByIdExternal(
            UUID idExternal
    ) {
        return repository
                .findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reading group not found with idExternal: "
                                        + idExternal
                        )
                );
    }

    private UserEntity findUserByIdExternal(
            UUID idExternal
    ) {
        return userRepository
                .findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with idExternal: "
                                        + idExternal
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

    private GroupMembershipState resolveMembershipState(
            UUID groupId,
            UUID userId
    ) {

        boolean isActiveMember =
                groupMemberRepository
                        .existsByGroupIdExternalAndUserIdExternalAndStatus(
                                groupId,
                                userId,
                                GroupMemberStatus.ACTIVE
                        );

        if (isActiveMember) {
            return GroupMembershipState.MEMBER;
        }

        boolean hasPendingJoinRequest =
                membershipRequestRepository
                        .existsByGroup_IdExternalAndTargetUser_IdExternalAndTypeAndStatus(
                                groupId,
                                userId,
                                GroupMembershipRequestType.JOIN_REQUEST,
                                GroupMembershipRequestStatus.PENDING
                        );

        if (hasPendingJoinRequest) {
            return GroupMembershipState.JOIN_REQUEST_PENDING;
        }

        boolean hasPendingInvitation =
                membershipRequestRepository
                        .existsByGroup_IdExternalAndTargetUser_IdExternalAndTypeAndStatus(
                                groupId,
                                userId,
                                GroupMembershipRequestType.INVITATION,
                                GroupMembershipRequestStatus.PENDING
                        );

        if (hasPendingInvitation) {
            return GroupMembershipState.INVITATION_PENDING;
        }

        return GroupMembershipState.NOT_MEMBER;
    }
}