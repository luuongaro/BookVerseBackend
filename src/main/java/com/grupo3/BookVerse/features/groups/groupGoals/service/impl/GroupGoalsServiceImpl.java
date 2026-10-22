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
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public GroupGoalsResponseDto createGoal(UUID groupId, GroupGoalsRequestDto requestDto) {
        ReadingGroupEntity group = findGroupByIdExternal(groupId);
        validateGroupIsActive(group);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageGoals(group, authenticatedUser);

        validateNoActiveGoal(groupId);
        validateGoalParams(requestDto, group);

        GroupGoalsEntity entity = groupGoalsMapper.toEntity(requestDto);
        entity.setGroup(group);
        entity.setStatus(GoalStatus.ACTIVE);

        GroupGoalsEntity saved = groupGoalsRepository.save(entity);
        return groupGoalsMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupGoalsResponseDto getGoalByIdExternal(UUID goalId) {
        GroupGoalsEntity goal = findGoalByIdExternal(goalId);
        validateGroupIsActive(goal.getGroup());

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanViewGoals(goal.getGroup(), authenticatedUser);

        return groupGoalsMapper.toResponseDto(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupGoalsResponseDto> getGoalsByGroupId(UUID groupId) {
        ReadingGroupEntity group = findGroupByIdExternal(groupId);
        validateGroupIsActive(group);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanViewGoals(group, authenticatedUser);

        List<GroupGoalsEntity> goals =
                groupGoalsRepository.findByGroup_IdExternalOrderByUpdatedAtDesc(groupId);

        return groupGoalsMapper.toResponseDtoList(goals);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupGoalsResponseDto getActiveGoalByGroupId(UUID groupId) {
        ReadingGroupEntity group = findGroupByIdExternal(groupId);
        validateGroupIsActive(group);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanViewGoals(group, authenticatedUser);

        GroupGoalsEntity activeGoal = groupGoalsRepository
                .findByGroup_IdExternalAndStatus(groupId, GoalStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active goal not found for group: " + groupId
                ));

        return groupGoalsMapper.toResponseDto(activeGoal);
    }

    @Override
    @Transactional
    public GroupGoalsResponseDto changeStatus(UUID goalId, GroupGoalStatusRequestDto requestDto) {
        GroupGoalsEntity goal = findGoalByIdExternal(goalId);
        validateGroupIsActive(goal.getGroup());

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageGoals(goal.getGroup(), authenticatedUser);

        validateStatusTransition(goal, requestDto.status());

        goal.setStatus(requestDto.status());

        GroupGoalsEntity saved = groupGoalsRepository.save(goal);
        return groupGoalsMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void cancelGoal(UUID goalId) {
        GroupGoalsEntity goal = findGoalByIdExternal(goalId);
        validateGroupIsActive(goal.getGroup());

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageGoals(goal.getGroup(), authenticatedUser);

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BadRequestException("Only active goals can be cancelled");
        }

        goal.setStatus(GoalStatus.CANCELLED);
        groupGoalsRepository.save(goal);
    }

    private void validateGoalParams(GroupGoalsRequestDto requestDto, ReadingGroupEntity group) {
        if (requestDto.targetProgress() == null || requestDto.targetProgress() <= 0) {
            throw new BadRequestException("Target progress must be greater than 0");
        }

        if (requestDto.goalType() == GoalType.PERCENTAGE && requestDto.targetProgress() > 100) {
            throw new BadRequestException("Percentage target progress cannot exceed 100");
        }

        if (group.getBook() != null && requestDto.goalType() == GoalType.CHAPTER) {
            throw new BadRequestException(
                    "Groups reading a book can only define PERCENTAGE goals"
            );
        }

        if (group.getStory() != null && requestDto.goalType() == GoalType.CHAPTER) {
            int totalChapters = group.getStory().getChapters() != null
                    ? group.getStory().getChapters().size()
                    : 0;

            if (totalChapters == 0) {
                throw new BadRequestException(
                        "Cannot define CHAPTER goals for a story without chapters"
                );
            }

            if (requestDto.targetProgress() > totalChapters) {
                throw new BadRequestException(
                        "Chapter target progress cannot exceed the total number of story chapters"
                );
            }
        }
    }

    private void validateNoActiveGoal(UUID groupId) {
        boolean hasActiveGoal = groupGoalsRepository.existsByGroup_IdExternalAndStatus(
                groupId,
                GoalStatus.ACTIVE
        );

        if (hasActiveGoal) {
            throw new BadRequestException(
                    "This reading group already has an active goal"
            );
        }
    }

    private void validateStatusTransition(GroupGoalsEntity goal, GoalStatus newStatus) {
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only active goals can change status"
            );
        }

        if (newStatus != GoalStatus.COMPLETED && newStatus != GoalStatus.CANCELLED) {
            throw new BadRequestException(
                    "Active goals can only become COMPLETED or CANCELLED"
            );
        }
    }

    private void validateCanManageGoals(ReadingGroupEntity group, UserEntity authenticatedUser) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isCreatorMember = groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                        group.getIdExternal(),
                        authenticatedUser.getIdExternal(),
                        GroupMemberType.CREATOR,
                        GroupMemberStatus.ACTIVE
                );

        boolean isOwner = group.getCreatedBy() != null
                && Objects.equals(group.getCreatedBy().getId(), authenticatedUser.getId());

        if (!isCreatorMember && !isOwner) {
            throw new AccessDeniedException(
                    "Only the group creator, an admin, or a moderator can manage goals"
            );
        }
    }

    private void validateCanViewGoals(ReadingGroupEntity group, UserEntity authenticatedUser) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isActiveMember = groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndStatus(
                        group.getIdExternal(),
                        authenticatedUser.getIdExternal(),
                        GroupMemberStatus.ACTIVE
                );

        boolean isOwner = group.getCreatedBy() != null
                && Objects.equals(group.getCreatedBy().getId(), authenticatedUser.getId());

        if (!isActiveMember && !isOwner) {
            throw new AccessDeniedException(
                    "Only active group members can view group goals"
            );
        }
    }

    private void validateGroupIsActive(ReadingGroupEntity group) {
        if (!Boolean.TRUE.equals(group.getIsActive())) {
            throw new BadRequestException("Inactive reading groups cannot manage goals");
        }
    }

    private boolean isAdminOrModerator(UserEntity user) {
        return user.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_MODERATOR"));
    }

    private GroupGoalsEntity findGoalByIdExternal(UUID idExternal) {
        return groupGoalsRepository
                .findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Group goal not found with idExternal: " + idExternal
                        )
                );
    }

    private ReadingGroupEntity findGroupByIdExternal(UUID groupId) {
        return readingGroupRepository
                .findByIdExternal(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reading group not found with idExternal: " + groupId
                        )
                );
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }

    @Override
    @Transactional
    public GroupGoalsResponseDto save(GroupGoalsRequestDto requestDto) {
        return createGoal(requestDto.groupId(), requestDto);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupGoalsResponseDto findById(UUID goalId) {
        return getGoalByIdExternal(goalId);
    }

    @Override
    @Transactional
    public void delete(UUID goalId) {
        cancelGoal(goalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupGoalsResponseDto> findAll() {
        List<GroupGoalsEntity> goals = groupGoalsRepository.findAll();
        return groupGoalsMapper.toResponseDtoList(goals);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupGoalsResponseDto findByGroupId(UUID groupId) {
        return getActiveGoalByGroupId(groupId);
    }
}