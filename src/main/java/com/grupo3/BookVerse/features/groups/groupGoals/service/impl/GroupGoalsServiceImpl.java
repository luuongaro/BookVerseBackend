package com.grupo3.BookVerse.features.groups.groupGoals.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalStatus;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalType;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GroupGoalsEntity;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalStatusRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsResponseDto;
import com.grupo3.BookVerse.features.groups.groupGoals.mappers.GroupGoalsMapper;
import com.grupo3.BookVerse.features.groups.groupGoals.repository.GroupGoalsRepository;
import com.grupo3.BookVerse.features.groups.groupGoals.service.GroupGoalsService;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupProgress.service.GroupProgressService;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
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
public class GroupGoalsServiceImpl implements GroupGoalsService {

    private final GroupGoalsRepository groupGoalsRepository;
    private final GroupGoalsMapper groupGoalsMapper;
    private final ReadingGroupRepository readingGroupRepository;
    private final GroupReadingCycleRepository groupReadingCycleRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupProgressService groupProgressService;

    @Override
    @Transactional
    public GroupGoalsResponseDto createGoal(
            UUID groupId,
            UUID cycleId,
            GroupGoalsRequestDto requestDto
    ) {
        ReadingGroupEntity group = findGroupByIdExternal(groupId);
        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle = findCycle(groupId, cycleId);
        validateCycleIsActive(cycle);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageGoals(group, authenticatedUser);

        validateNoActiveGoal(cycleId);
        validateGoalParams(requestDto, cycle);

        if (requestDto.goalType() == GoalType.PERCENTAGE) {
            validatePercentageGoal(
                    groupId,
                    cycleId,
                    requestDto.targetProgress()
            );
        }

        GroupGoalsEntity entity =
                groupGoalsMapper.toEntity(requestDto);

        // Relación temporal con el grupo.
        // Se eliminará cuando termine la migración completa.
        entity.setGroup(group);

        entity.setReadingCycle(cycle);
        entity.setStatus(GoalStatus.ACTIVE);

        GroupGoalsEntity saved =
                groupGoalsRepository.save(entity);

        return groupGoalsMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupGoalsResponseDto getGoalByIdExternal(
            UUID groupId,
            UUID cycleId,
            UUID goalId
    ) {
        ReadingGroupEntity group =
                findGroupByIdExternal(groupId);

        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle =
                findCycle(groupId, cycleId);

        GroupGoalsEntity goal =
                findGoal(groupId, cycleId, goalId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanViewGoals(
                cycle.getGroup(),
                authenticatedUser
        );

        return groupGoalsMapper.toResponseDto(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupGoalsResponseDto> getGoalsByCycleId(
            UUID groupId,
            UUID cycleId
    ) {
        ReadingGroupEntity group =
                findGroupByIdExternal(groupId);

        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle =
                findCycle(groupId, cycleId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanViewGoals(
                cycle.getGroup(),
                authenticatedUser
        );

        List<GroupGoalsEntity> goals =
                groupGoalsRepository
                        .findByReadingCycle_IdExternalOrderByUpdatedAtDesc(
                                cycleId
                        );

        return groupGoalsMapper.toResponseDtoList(goals);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupGoalsResponseDto getActiveGoalByCycleId(
            UUID groupId,
            UUID cycleId
    ) {
        ReadingGroupEntity group =
                findGroupByIdExternal(groupId);

        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle =
                findCycle(groupId, cycleId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanViewGoals(
                cycle.getGroup(),
                authenticatedUser
        );

        GroupGoalsEntity activeGoal =
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

        return groupGoalsMapper.toResponseDto(activeGoal);
    }

    @Override
    @Transactional
    public GroupGoalsResponseDto changeStatus(
            UUID groupId,
            UUID cycleId,
            UUID goalId,
            GroupGoalStatusRequestDto requestDto
    ) {
        ReadingGroupEntity group =
                findGroupByIdExternal(groupId);

        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle =
                findCycle(groupId, cycleId);

        GroupGoalsEntity goal =
                findGoal(groupId, cycleId, goalId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanManageGoals(
                cycle.getGroup(),
                authenticatedUser
        );

        validateStatusTransition(
                goal,
                requestDto.status()
        );

        goal.setStatus(requestDto.status());

        GroupGoalsEntity saved =
                groupGoalsRepository.save(goal);

        return groupGoalsMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void cancelGoal(
            UUID groupId,
            UUID cycleId,
            UUID goalId
    ) {
        ReadingGroupEntity group =
                findGroupByIdExternal(groupId);

        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle =
                findCycle(groupId, cycleId);

        GroupGoalsEntity goal =
                findGoal(groupId, cycleId, goalId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateCanManageGoals(
                cycle.getGroup(),
                authenticatedUser
        );

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only active goals can be cancelled"
            );
        }

        goal.setStatus(GoalStatus.CANCELLED);
        groupGoalsRepository.save(goal);
    }

    private void validateGoalParams(
            GroupGoalsRequestDto requestDto,
            GroupReadingCycleEntity cycle
    ) {
        if (requestDto.targetProgress() == null
                || requestDto.targetProgress() <= 0) {
            throw new BadRequestException(
                    "Target progress must be greater than 0"
            );
        }

        if (requestDto.goalType() == GoalType.PERCENTAGE) {
            if (requestDto.targetProgress() > 100) {
                throw new BadRequestException(
                        "Percentage target progress cannot exceed 100"
                );
            }

            return;
        }

        if (requestDto.goalType() != GoalType.CHAPTER) {
            return;
        }

        int totalChapters =
                getTotalChaptersForCycle(cycle);

        if (totalChapters <= 0) {
            throw new BadRequestException(
                    "Cannot create a chapter goal without a valid chapter reference"
            );
        }

        if (requestDto.targetProgress() > totalChapters) {
            throw new BadRequestException(
                    "Chapter target cannot exceed the reading cycle's total chapters"
            );
        }
    }

    private void validatePercentageGoal(
            UUID groupId,
            UUID cycleId,
            Integer requestedTarget
    ) {
        double currentAveragePercentage =
                groupProgressService
                        .calculateAveragePercentage(
                                groupId,
                                cycleId
                        );

        int highestCompletedTarget =
                findHighestCompletedPercentageTarget(
                        cycleId
                );

        double highestReference =
                Math.max(
                        currentAveragePercentage,
                        highestCompletedTarget
                );

        int minimumAllowed =
                (int) Math.floor(highestReference) + 1;

        if (minimumAllowed > 100) {
            throw new BadRequestException(
                    "A new percentage goal cannot be created because "
                            + "the reading cycle has already reached 100%"
            );
        }

        if (requestedTarget < minimumAllowed) {
            throw new BadRequestException(
                    "Percentage target must be at least "
                            + minimumAllowed
                            + "%. Current group progress is "
                            + formatPercentage(currentAveragePercentage)
                            + "% and the highest completed percentage goal is "
                            + highestCompletedTarget
                            + "%."
            );
        }
    }

    private int findHighestCompletedPercentageTarget(
            UUID cycleId
    ) {
        List<GroupGoalsEntity> completedPercentageGoals =
                groupGoalsRepository
                        .findByReadingCycle_IdExternalAndGoalTypeAndStatusOrderByTargetProgressDesc(
                                cycleId,
                                GoalType.PERCENTAGE,
                                GoalStatus.COMPLETED
                        );

        return completedPercentageGoals.stream()
                .map(GroupGoalsEntity::getTargetProgress)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(0);
    }

    private String formatPercentage(double percentage) {
        return String.format(
                java.util.Locale.US,
                "%.2f",
                percentage
        );
    }

    private int getTotalChaptersForCycle(
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

    private void validateNoActiveGoal(UUID cycleId) {
        boolean hasActiveGoal =
                groupGoalsRepository
                        .existsByReadingCycle_IdExternalAndStatus(
                                cycleId,
                                GoalStatus.ACTIVE
                        );

        if (hasActiveGoal) {
            throw new BadRequestException(
                    "This reading cycle already has an active goal"
            );
        }
    }

    private void validateStatusTransition(
            GroupGoalsEntity goal,
            GoalStatus newStatus
    ) {
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only active goals can change status"
            );
        }

        if (newStatus != GoalStatus.COMPLETED
                && newStatus != GoalStatus.CANCELLED) {
            throw new BadRequestException(
                    "Active goals can only become COMPLETED or CANCELLED"
            );
        }
    }

    private void validateCycleIsActive(
            GroupReadingCycleEntity cycle
    ) {
        if (cycle.getStatus()
                != GroupReadingCycleStatus.ACTIVE) {
            throw new BadRequestException(
                    "Goals can only be created for an active reading cycle"
            );
        }
    }

    private void validateCanManageGoals(
            ReadingGroupEntity group,
            UserEntity authenticatedUser
    ) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isCreatorMember =
                groupMemberRepository
                        .existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                                group.getIdExternal(),
                                authenticatedUser.getIdExternal(),
                                GroupMemberType.CREATOR,
                                GroupMemberStatus.ACTIVE
                        );

        boolean isOwner =
                group.getCreatedBy() != null
                        && Objects.equals(
                        group.getCreatedBy().getId(),
                        authenticatedUser.getId()
                );

        if (!isCreatorMember && !isOwner) {
            throw new AccessDeniedException(
                    "Only the group creator, an admin, or a moderator can manage goals"
            );
        }
    }

    private void validateCanViewGoals(
            ReadingGroupEntity group,
            UserEntity authenticatedUser
    ) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isActiveMember =
                groupMemberRepository
                        .existsByGroupIdExternalAndUserIdExternalAndStatus(
                                group.getIdExternal(),
                                authenticatedUser.getIdExternal(),
                                GroupMemberStatus.ACTIVE
                        );

        boolean isOwner =
                group.getCreatedBy() != null
                        && Objects.equals(
                        group.getCreatedBy().getId(),
                        authenticatedUser.getId()
                );

        if (!isActiveMember && !isOwner) {
            throw new AccessDeniedException(
                    "Only active group members can view group goals"
            );
        }
    }

    private void validateGroupIsActive(
            ReadingGroupEntity group
    ) {
        if (!Boolean.TRUE.equals(group.getIsActive())) {
            throw new BadRequestException(
                    "Inactive reading groups cannot manage goals"
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

    private GroupGoalsEntity findGoal(
            UUID groupId,
            UUID cycleId,
            UUID goalId
    ) {
        return groupGoalsRepository
                .findByIdExternalAndReadingCycle_IdExternalAndReadingCycle_Group_IdExternal(
                        goalId,
                        cycleId,
                        groupId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Group goal not found for the specified reading cycle"
                        )
                );
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

    private ReadingGroupEntity findGroupByIdExternal(
            UUID groupId
    ) {
        return readingGroupRepository
                .findByIdExternal(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reading group not found with idExternal: "
                                        + groupId
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