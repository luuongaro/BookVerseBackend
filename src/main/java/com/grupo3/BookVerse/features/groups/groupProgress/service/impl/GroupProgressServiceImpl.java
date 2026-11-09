package com.grupo3.BookVerse.features.groups.groupProgress.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalStatus;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalType;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GroupGoalsEntity;
import com.grupo3.BookVerse.features.groups.groupGoals.repository.GroupGoalsRepository;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupProgress.dto.GroupProgressResponseDto;
import com.grupo3.BookVerse.features.groups.groupProgress.service.GroupProgressService;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.status.domain.ProgressType;
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupProgressServiceImpl implements GroupProgressService {

    private static final Set<ReadingStatusEnum> VALID_PROGRESS_STATUSES = Set.of(
            ReadingStatusEnum.IN_PROGRESS,
            ReadingStatusEnum.FINISHED,
            ReadingStatusEnum.RE_READING
    );

    private final ReadingGroupRepository readingGroupRepository;
    private final GroupGoalsRepository groupGoalsRepository;
    private final ReadingStatusRepository readingStatusRepository;
    private final GroupReadingCycleRepository groupReadingCycleRepository;

    @Override
    @Transactional(readOnly = true)
    public GroupProgressResponseDto calculateProgress(
            UUID groupId,
            UUID cycleId
    ) {
        ReadingGroupEntity group =
                findGroupByIdExternal(groupId);

        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle =
                findCycle(groupId, cycleId);

        if (cycle.getStatus() != GroupReadingCycleStatus.ACTIVE) {
            throw new BadRequestException(
                    "Group progress can only be calculated for an active reading cycle"
            );
        }

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanViewGroupProgress(
                group,
                authenticatedUser
        );

        GroupGoalsEntity goal =
                groupGoalsRepository
                        .findByReadingCycle_IdExternalAndStatus(
                                cycleId,
                                GoalStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Active goal not found for reading cycle: "
                                                + cycleId
                                )
                        );

        List<UUID> activeMemberIds =
                group.getMembers()
                        .stream()
                        .filter(member ->
                                member.getStatus()
                                        == GroupMemberStatus.ACTIVE
                        )
                        .map(GroupMemberEntity::getUser)
                        .filter(Objects::nonNull)
                        .map(UserEntity::getIdExternal)
                        .toList();

        if (activeMemberIds.isEmpty()) {
            return buildResponse(
                    groupId,
                    cycleId,
                    goal,
                    0.0
            );
        }

        List<ReadingStatusEntity> statuses =
                getCycleReadingStatuses(
                        cycle,
                        activeMemberIds
                )
                        .stream()
                        .filter(status ->
                                VALID_PROGRESS_STATUSES.contains(
                                        status.getStatus()
                                )
                        )
                        .toList();

        double averageProgress;

        if (goal.getGoalType() == GoalType.PERCENTAGE) {
            averageProgress =
                    calculatePercentageProgress(
                            statuses,
                            cycle,
                            activeMemberIds.size()
                    );
        } else {
            averageProgress =
                    calculateChapterProgress(
                            statuses,
                            cycle,
                            activeMemberIds.size()
                    );
        }

        return buildResponse(
                groupId,
                cycleId,
                goal,
                averageProgress
        );
    }

    @Override
    @Transactional(readOnly = true)
    public double calculateAveragePercentage(
            UUID groupId,
            UUID cycleId
    ) {
        ReadingGroupEntity group = findGroupByIdExternal(groupId);
        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle = findCycle(
                groupId,
                cycleId
        );

        if (cycle.getStatus() != GroupReadingCycleStatus.ACTIVE) {
            throw new BadRequestException(
                    "Group progress can only be calculated for an active reading cycle"
            );
        }



        List<UUID> activeMemberIds = group.getMembers().stream()
                .filter(member ->
                        member.getStatus() == GroupMemberStatus.ACTIVE
                )
                .map(GroupMemberEntity::getUser)
                .filter(Objects::nonNull)
                .map(UserEntity::getIdExternal)
                .toList();

        if (activeMemberIds.isEmpty()) {
            return 0.0;
        }

        List<ReadingStatusEntity> statuses =
                getCycleReadingStatuses(
                        cycle,
                        activeMemberIds
                ).stream()
                        .filter(status ->
                                VALID_PROGRESS_STATUSES.contains(
                                        status.getStatus()
                                )
                        )
                        .toList();

        double totalPercentage = statuses.stream()
                .mapToDouble(status ->
                        normalizeToPercentage(status, cycle)
                )
                .sum();

        return totalPercentage / activeMemberIds.size();
    }

    private double calculateChapterProgress(
            List<ReadingStatusEntity> statuses,
            GroupReadingCycleEntity cycle,
            int memberCount
    ) {
        if (memberCount == 0) {
            return 0.0;
        }

        int totalChapters =
                getCycleTotalChapters(cycle);

        if (totalChapters <= 0) {
            throw new BadRequestException(
                    "A valid total chapter reference is required to calculate chapter progress"
            );
        }

        double total = 0.0;

        for (ReadingStatusEntity status : statuses) {

            if (status.getProgressType() == null
                    || status.getProgressValue() == null) {
                continue;
            }

            if (status.getProgressType() == ProgressType.CHAPTER) {
                total += Math.max(
                        0.0,
                        Math.min(
                                status.getProgressValue(),
                                totalChapters
                        )
                );
            }

            if (status.getProgressType() == ProgressType.PERCENTAGE) {
                double chapterEquivalent =
                        totalChapters
                                * status.getProgressValue()
                                / 100.0;

                total += Math.min(
                        chapterEquivalent,
                        totalChapters
                );
            }
        }

        return total / memberCount;
    }

    private double calculatePercentageProgress(
            List<ReadingStatusEntity> statuses,
            GroupReadingCycleEntity cycle,
            int memberCount
    ) {
        if (memberCount == 0) {
            return 0.0;
        }

        double total =
                statuses.stream()
                        .mapToDouble(status ->
                                normalizeToPercentage(
                                        status,
                                        cycle
                                )
                        )
                        .sum();

        return total / memberCount;
    }
    private double normalizeToPercentage(
            ReadingStatusEntity status,
            GroupReadingCycleEntity cycle
    ) {
        if (status.getProgressType() == null
                || status.getProgressValue() == null) {
            return 0.0;
        }

        if (status.getProgressType() == ProgressType.PERCENTAGE) {
            return Math.max(
                    0.0,
                    Math.min(
                            status.getProgressValue().doubleValue(),
                            100.0
                    )
            );
        }

        if (status.getProgressType() == ProgressType.CHAPTER) {

            int totalChapters = getCycleTotalChapters(cycle);

            if (totalChapters <= 0) {
                throw new BadRequestException(
                        "A valid total chapter reference is required to normalize chapter progress"
                );
            }

            double percentage =
                    status.getProgressValue() * 100.0
                            / totalChapters;

            return Math.max(
                    0.0,
                    Math.min(percentage, 100.0)
            );
        }

        return 0.0;
    }

    private int getCycleTotalChapters(
            GroupReadingCycleEntity cycle
    ) {
        if (cycle.getBook() != null) {

            if (cycle.getTotalChapters() == null
                    || cycle.getTotalChapters() <= 0) {
                throw new BadRequestException(
                        "Book reading cycle does not have a valid total chapter reference"
                );
            }

            return cycle.getTotalChapters();
        }

        if (cycle.getStory() != null) {

            if (cycle.getStory().getChapters() == null) {
                return 0;
            }

            return (int) cycle.getStory()
                    .getChapters()
                    .stream()
                    .filter(chapter ->
                            chapter.isPublished()
                                    && !chapter.isDeleted()
                    )
                    .count();
        }

        throw new BadRequestException(
                "Reading cycle must be associated with either a book or a story"
        );
    }
    private List<ReadingStatusEntity> getCycleReadingStatuses(
            GroupReadingCycleEntity cycle,
            List<UUID> memberIds
    ) {
        if (cycle.getBook() != null) {
            return readingStatusRepository
                    .findByUserIdExternalInAndBookIdExternal(
                            memberIds,
                            cycle.getBook().getIdExternal()
                    );
        }

        if (cycle.getStory() != null) {
            return readingStatusRepository
                    .findByUserIdExternalInAndStoryIdExternal(
                            memberIds,
                            cycle.getStory().getIdExternal()
                    );
        }

        throw new BadRequestException(
                "Reading cycle must be associated with either a book or a story"
        );
    }

    private GroupProgressResponseDto buildResponse(
            UUID groupId,
            UUID cycleId,
            GroupGoalsEntity goal,
            double averageProgress
    ) {
        boolean achieved =
                averageProgress >= goal.getTargetProgress();

        return GroupProgressResponseDto.builder()
                .groupId(groupId)
                .readingCycleId(cycleId)
                .goalType(goal.getGoalType())
                .currentProgress(averageProgress)
                .targetProgress(goal.getTargetProgress())
                .achieved(achieved)
                .build();
    }

    private void validateGroupIsActive(ReadingGroupEntity group) {
        if (!Boolean.TRUE.equals(group.getIsActive())) {
            throw new BadRequestException(
                    "Inactive reading groups cannot calculate progress"
            );
        }
    }

    private void validateCanViewGroupProgress(
            ReadingGroupEntity group,
            UserEntity authenticatedUser
    ) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isOwner = group.getCreatedBy() != null
                && Objects.equals(group.getCreatedBy().getId(), authenticatedUser.getId());

        boolean isActiveMember = group.getMembers().stream()
                .anyMatch(member ->
                        member.getStatus() == GroupMemberStatus.ACTIVE
                                && member.getUser() != null
                                && Objects.equals(member.getUser().getId(), authenticatedUser.getId())
                );

        if (!isOwner && !isActiveMember) {
            throw new AccessDeniedException(
                    "You do not have permission to view this group's progress"
            );
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
                        "Reading group not found with idExternal: " + groupId
                ));
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }

    private GroupReadingCycleEntity findCycle(
            UUID groupId,
            UUID cycleId
    ) {
        return groupReadingCycleRepository
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
}
