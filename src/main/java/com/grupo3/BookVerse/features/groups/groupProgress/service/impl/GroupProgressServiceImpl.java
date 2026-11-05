package com.grupo3.BookVerse.features.groups.groupProgress.service.impl;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalType;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GroupGoalsEntity;
import com.grupo3.BookVerse.features.groups.groupGoals.repository.GroupGoalsRepository;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupProgress.dto.GroupProgressResponseDto;
import com.grupo3.BookVerse.features.groups.groupProgress.service.GroupProgressService;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.status.domain.ProgressType;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEnum;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalStatus;
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

    @Override
    @Transactional(readOnly = true)
    public GroupProgressResponseDto calculateProgress(UUID groupId) {
        ReadingGroupEntity group = findGroupByIdExternal(groupId);
        validateGroupIsActive(group);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanViewGroupProgress(group, authenticatedUser);

        GroupGoalsEntity goal = groupGoalsRepository
                .findByGroup_IdExternalAndStatus(groupId, GoalStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active goal not found for group: " + groupId
                ));

        List<UUID> activeMemberIds = group.getMembers().stream()
                .filter(member -> member.getStatus() == GroupMemberStatus.ACTIVE)
                .map(GroupMemberEntity::getUser)
                .filter(Objects::nonNull)
                .map(UserEntity::getIdExternal)
                .toList();

        if (activeMemberIds.isEmpty()) {
            return buildResponse(groupId, goal, 0.0);
        }

        List<ReadingStatusEntity> statuses = getGroupReadingStatuses(group, activeMemberIds).stream()
                .filter(status -> VALID_PROGRESS_STATUSES.contains(status.getStatus()))
                .toList();

        double totalProgress = 0.0;

        for (ReadingStatusEntity status : statuses) {
            totalProgress += normalize(status, group, goal);
        }

        double averageProgress = totalProgress / activeMemberIds.size();

        return buildResponse(groupId, goal, averageProgress);
    }

    private List<ReadingStatusEntity> getGroupReadingStatuses(
            ReadingGroupEntity group,
            List<UUID> memberIds
    ) {
        if (group.getBook() != null) {
            return readingStatusRepository.findByUserIdExternalInAndBookIdExternal(
                    memberIds,
                    group.getBook().getIdExternal()
            );
        }

        if (group.getStory() != null) {
            return readingStatusRepository.findByUserIdExternalInAndStoryIdExternal(
                    memberIds,
                    group.getStory().getIdExternal()
            );
        }

        throw new BadRequestException(
                "Group must be associated with either a book or a story"
        );
    }

    private double normalize(
            ReadingStatusEntity status,
            ReadingGroupEntity group,
            GroupGoalsEntity goal
    ) {
        if (status.getProgressType() == null || status.getProgressValue() == null) {
            return 0.0;
        }

        int totalChapters = getTotalChapters(group);
        ProgressType progressType = status.getProgressType();

        if (goal.getGoalType() == GoalType.PERCENTAGE) {
            if (progressType == ProgressType.PERCENTAGE) {
                return status.getProgressValue().doubleValue();
            }

            return totalChapters == 0
                    ? 0.0
                    : status.getProgressValue().doubleValue() * 100.0 / totalChapters;
        }

        if (progressType == ProgressType.CHAPTER) {
            return status.getProgressValue().doubleValue();
        }

        return totalChapters == 0
                ? 0.0
                : totalChapters * status.getProgressValue().doubleValue() / 100.0;
    }

    private int getTotalChapters(ReadingGroupEntity group) {
        if (group.getBook() != null) {
            return group.getTotalChapters() == null
                    ? 0
                    : group.getTotalChapters();
        }

        if (group.getStory() == null || group.getStory().getChapters() == null) {
            return 0;
        }

        return (int) group.getStory().getChapters().stream()
                .filter(chapter -> chapter.isPublished() && !chapter.isDeleted())
                .count();
    }

    private GroupProgressResponseDto buildResponse(
            UUID groupId,
            GroupGoalsEntity goal,
            double averageProgress
    ) {
        boolean achieved = averageProgress >= goal.getTargetProgress();

        return GroupProgressResponseDto.builder()
                .groupId(groupId)
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
}
