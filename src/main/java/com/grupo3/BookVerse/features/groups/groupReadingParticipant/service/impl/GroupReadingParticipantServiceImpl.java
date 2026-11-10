package com.grupo3.BookVerse.features.groups.groupReadingParticipant.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantEntity;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantStatus;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.dto.GroupReadingParticipantResponseDto;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.mappers.GroupReadingParticipantMapper;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.repository.GroupReadingParticipantRepository;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.service.GroupReadingParticipantService;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEnum;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupReadingParticipantServiceImpl
        implements GroupReadingParticipantService {

    private final GroupReadingParticipantRepository participantRepository;
    private final GroupReadingParticipantMapper participantMapper;
    private final ReadingStatusRepository readingStatusRepository;
    private final ReadingGroupRepository readingGroupRepository;
    private final GroupReadingCycleRepository cycleRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public GroupReadingParticipantResponseDto joinCycle(
            UUID groupId,
            UUID cycleId
    ) {
        ReadingGroupEntity group =
                findActiveGroup(groupId);

        GroupReadingCycleEntity cycle =
                findCycle(groupId, cycleId);

        validateCycleIsActive(cycle);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        GroupMemberEntity membership =
                findActiveMembership(
                        group.getIdExternal(),
                        authenticatedUser.getIdExternal()
                );

        ReadingStatusEntity readingStatus =
                findValidReadingStatusForParticipation(
                        cycle,
                        authenticatedUser
                );

        GroupReadingParticipantStatus participationStatus =
                readingStatus.getStatus() == ReadingStatusEnum.FINISHED
                        ? GroupReadingParticipantStatus.COMPLETED
                        : GroupReadingParticipantStatus.ACTIVE;

        GroupReadingParticipantEntity existing =
                participantRepository
                        .findByReadingCycle_IdExternalAndGroupMember_User_IdExternal(
                                cycleId,
                                authenticatedUser.getIdExternal()
                        )
                        .orElse(null);


        if (existing != null) {

            if (existing.getStatus()
                    == GroupReadingParticipantStatus.ACTIVE) {
                throw new BadRequestException(
                        "User is already participating in this reading cycle"
                );
            }

            if (existing.getStatus()
                    == GroupReadingParticipantStatus.COMPLETED) {
                throw new BadRequestException(
                        "A completed participation cannot be reactivated"
                );
            }

            existing.setStatus(participationStatus);

            existing.setCompletedAt(
                    participationStatus == GroupReadingParticipantStatus.COMPLETED
                            ? LocalDateTime.now()
                            : null
            );

            existing.setJoinedAt(LocalDateTime.now());

            GroupReadingParticipantEntity saved =
                    participantRepository.save(existing);

            return participantMapper.toResponseDto(saved);
        }

        GroupReadingParticipantEntity participant =
                GroupReadingParticipantEntity.builder()
                        .readingCycle(cycle)
                        .groupMember(membership)
                        .status(participationStatus)
                        .completedAt(
                                participationStatus
                                        == GroupReadingParticipantStatus.COMPLETED
                                        ? LocalDateTime.now()
                                        : null
                        )
                        .build();

        GroupReadingParticipantEntity saved =
                participantRepository.save(participant);

        return participantMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void leaveCycle(
            UUID groupId,
            UUID cycleId
    ) {
        findActiveGroup(groupId);

        GroupReadingCycleEntity cycle =
                findCycle(groupId, cycleId);

        validateCycleIsActive(cycle);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        findActiveMembership(
                groupId,
                authenticatedUser.getIdExternal()
        );

        GroupReadingParticipantEntity participant =
                findParticipant(
                        cycleId,
                        authenticatedUser.getIdExternal()
                );

        if (participant.getStatus()
                != GroupReadingParticipantStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only active participants can leave a reading cycle"
            );
        }

        participant.setStatus(
                GroupReadingParticipantStatus.WITHDRAWN
        );

        participantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupReadingParticipantResponseDto> getParticipants(
            UUID groupId,
            UUID cycleId
    ) {
        findActiveGroup(groupId);
        findCycle(groupId, cycleId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanViewParticipants(
                groupId,
                authenticatedUser
        );

        List<GroupReadingParticipantEntity> participants =
                participantRepository
                        .findByReadingCycle_IdExternal(
                                cycleId
                        );

        return participantMapper.toResponseDtoList(
                participants
        );
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

        if (!Boolean.TRUE.equals(group.getIsActive())) {
            throw new BadRequestException(
                    "Reading group is inactive"
            );
        }

        return group;
    }

    private GroupReadingCycleEntity findCycle(
            UUID groupId,
            UUID cycleId
    ) {
        return cycleRepository
                .findByIdExternalAndGroup_IdExternal(
                        cycleId,
                        groupId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reading cycle not found for group: "
                                        + groupId
                        )
                );
    }

    private GroupMemberEntity findActiveMembership(
            UUID groupId,
            UUID userId
    ) {
        GroupMemberEntity membership =
                groupMemberRepository
                        .findByGroupIdExternalAndUserIdExternal(
                                groupId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Active group membership not found"
                                )
                        );

        if (membership.getStatus()
                != GroupMemberStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Only active group members can participate in a reading cycle"
            );
        }

        return membership;
    }

    private GroupReadingParticipantEntity findParticipant(
            UUID cycleId,
            UUID userId
    ) {
        return participantRepository
                .findByReadingCycle_IdExternalAndGroupMember_User_IdExternal(
                        cycleId,
                        userId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reading cycle participation not found"
                        )
                );
    }

    private void validateCycleIsActive(
            GroupReadingCycleEntity cycle
    ) {
        if (cycle.getStatus()
                != GroupReadingCycleStatus.ACTIVE) {
            throw new BadRequestException(
                    "Participation is only available for active reading cycles"
            );
        }
    }

    private void validateCanViewParticipants(
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
                    "Only active group members can view reading participants"
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

    @Override
    @Transactional
    public void synchronizeWithReadingStatus(
            ReadingStatusEntity readingStatus
    ) {
        if (readingStatus == null
                || readingStatus.getUser() == null) {
            return;
        }

        List<GroupReadingParticipantEntity> participations;

        if (readingStatus.getBook() != null) {

            participations =
                    participantRepository
                            .findByGroupMember_User_IdExternalAndReadingCycle_Book_IdExternal(
                                    readingStatus.getUser().getIdExternal(),
                                    readingStatus.getBook().getIdExternal()
                            );

        } else if (readingStatus.getStory() != null) {

            participations =
                    participantRepository
                            .findByGroupMember_User_IdExternalAndReadingCycle_Story_IdExternal(
                                    readingStatus.getUser().getIdExternal(),
                                    readingStatus.getStory().getIdExternal()
                            );

        } else {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (GroupReadingParticipantEntity participant : participations) {

            // No modificamos ciclos históricos.
            if (participant.getReadingCycle().getStatus()
                    != GroupReadingCycleStatus.ACTIVE) {
                continue;
            }

            if (readingStatus.getStatus()
                    == ReadingStatusEnum.FINISHED) {

                participant.setStatus(
                        GroupReadingParticipantStatus.COMPLETED
                );

                if (participant.getCompletedAt() == null) {
                    participant.setCompletedAt(now);
                }

                continue;
            }

            if (readingStatus.getStatus()
                    == ReadingStatusEnum.ABANDONED) {

                participant.setStatus(
                        GroupReadingParticipantStatus.WITHDRAWN
                );

                participant.setCompletedAt(null);
                continue;
            }

            if (readingStatus.getStatus()
                    == ReadingStatusEnum.IN_PROGRESS
                    || readingStatus.getStatus()
                    == ReadingStatusEnum.RE_READING) {

                if (participant.getStatus()
                        != GroupReadingParticipantStatus.WITHDRAWN) {

                    participant.setStatus(
                            GroupReadingParticipantStatus.ACTIVE
                    );

                    participant.setCompletedAt(null);
                }
            }
        }

        participantRepository.saveAll(participations);
    }

    @Override
    @Transactional
    public void withdrawForDeletedReadingStatus(
            ReadingStatusEntity readingStatus
    ) {
        if (readingStatus == null
                || readingStatus.getUser() == null) {
            return;
        }

        List<GroupReadingParticipantEntity> participations;

        if (readingStatus.getBook() != null) {

            participations =
                    participantRepository
                            .findByGroupMember_User_IdExternalAndReadingCycle_Book_IdExternal(
                                    readingStatus.getUser().getIdExternal(),
                                    readingStatus.getBook().getIdExternal()
                            );

        } else if (readingStatus.getStory() != null) {

            participations =
                    participantRepository
                            .findByGroupMember_User_IdExternalAndReadingCycle_Story_IdExternal(
                                    readingStatus.getUser().getIdExternal(),
                                    readingStatus.getStory().getIdExternal()
                            );

        } else {
            return;
        }

        for (GroupReadingParticipantEntity participant : participations) {

            if (participant.getReadingCycle() == null
                    || participant.getReadingCycle().getStatus()
                    != GroupReadingCycleStatus.ACTIVE) {
                continue;
            }

            if (participant.getStatus()
                    == GroupReadingParticipantStatus.ACTIVE) {

                participant.setStatus(
                        GroupReadingParticipantStatus.WITHDRAWN
                );

                participant.setCompletedAt(null);
            }
        }

        participantRepository.saveAll(participations);
    }

    private ReadingStatusEntity findValidReadingStatusForParticipation(
            GroupReadingCycleEntity cycle,
            UserEntity user
    ) {
        ReadingStatusEntity readingStatus;

        if (cycle.getBook() != null) {
            readingStatus = readingStatusRepository
                    .findByUserIdExternalAndBookIdExternal(
                            user.getIdExternal(),
                            cycle.getBook().getIdExternal()
                    )
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "You must add the cycle's book to your library before participating"
                            )
                    );

        } else if (cycle.getStory() != null) {

            readingStatus = readingStatusRepository
                    .findByUserIdExternalAndStoryIdExternal(
                            user.getIdExternal(),
                            cycle.getStory().getIdExternal()
                    )
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "You must add the cycle's story to your library before participating"
                            )
                    );

        } else {
            throw new BadRequestException(
                    "Reading cycle must be associated with either a book or a story"
            );
        }

        if (!VALID_PARTICIPATION_STATUSES.contains(
                readingStatus.getStatus()
        )) {
            throw new BadRequestException(
                    "The reading must be IN_PROGRESS, RE_READING, or FINISHED before joining the cycle"
            );
        }

        return readingStatus;
    }

    private static final Set<ReadingStatusEnum> VALID_PARTICIPATION_STATUSES =
            Set.of(
                    ReadingStatusEnum.IN_PROGRESS,
                    ReadingStatusEnum.RE_READING,
                    ReadingStatusEnum.FINISHED
            );
    }

