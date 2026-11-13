package com.grupo3.BookVerse.features.groups.groupComment.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentEntity;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentStatus;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentRequestDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentResponseDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentsVisibilityResponseDto;
import com.grupo3.BookVerse.features.groups.groupComment.mappers.GroupCommentMapper;
import com.grupo3.BookVerse.features.groups.groupComment.repository.GroupCommentRepository;
import com.grupo3.BookVerse.features.groups.groupComment.service.GroupCommentService;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantEntity;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantStatus;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.repository.GroupReadingParticipantRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.status.domain.ProgressType;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEnum;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupCommentServiceImpl implements GroupCommentService {

    private static final Set<ReadingStatusEnum> VALID_READING_STATUSES = Set.of(
            ReadingStatusEnum.IN_PROGRESS,
            ReadingStatusEnum.RE_READING,
            ReadingStatusEnum.FINISHED
    );

    private static final Set<GroupReadingParticipantStatus>
            VALID_PARTICIPANT_STATUSES = Set.of(
            GroupReadingParticipantStatus.ACTIVE,
            GroupReadingParticipantStatus.COMPLETED
    );

    private final GroupCommentRepository groupCommentRepository;
    private final GroupCommentMapper groupCommentMapper;
    private final ReadingGroupRepository readingGroupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ReadingStatusRepository readingStatusRepository;
    private final GroupReadingCycleRepository groupReadingCycleRepository;
    private final GroupReadingParticipantRepository participantRepository;

    @Override
    @Transactional
    public GroupCommentResponseDto createComment(
            UUID groupId,
            GroupCommentRequestDto requestDto
    ) {
        ReadingGroupEntity group =
                findGroupByIdExternal(groupId);

        GroupReadingCycleEntity activeCycle =
                findActiveCycle(groupId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        validateActiveMembership(
                groupId,
                authenticatedUser
        );

        validateActiveParticipation(
                activeCycle.getIdExternal(),
                authenticatedUser
        );

        ReadingStatusEntity readingStatus =
                findReadingStatusForCycle(
                        activeCycle,
                        authenticatedUser
                );

        validateReadingStatus(readingStatus);

        validateProgressReference(
                requestDto.getProgressType(),
                requestDto.getProgressValue(),
                activeCycle
        );

        double currentProgressPercentage =
                normalizeToPercentage(
                        readingStatus.getProgressType(),
                        readingStatus.getProgressValue(),
                        activeCycle
                );

        double commentProgressPercentage =
                normalizeToPercentage(
                        requestDto.getProgressType(),
                        requestDto.getProgressValue(),
                        activeCycle
                );

        if (commentProgressPercentage > currentProgressPercentage) {
            throw new BadRequestException(
                    "You cannot comment beyond your current reading progress"
            );
        }

        GroupCommentEntity comment =
                groupCommentMapper.toEntity(requestDto);

        comment.setGroup(group);
        comment.setReadingCycle(activeCycle);
        comment.setUser(authenticatedUser);
        comment.setStatus(GroupCommentStatus.ACTIVE);
        comment.setIsHidden(false);

        GroupCommentEntity savedComment =
                groupCommentRepository.save(comment);

        return groupCommentMapper.toResponseDto(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupCommentsVisibilityResponseDto getVisibleCommentsByGroupId(
            UUID groupId
    ) {
        findGroupByIdExternal(groupId);

        GroupReadingCycleEntity activeCycle =
                findActiveCycle(groupId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        if (isAdminOrModerator(authenticatedUser)) {
            List<GroupCommentEntity> activeComments =
                    findActiveCommentsForCycle(activeCycle);

            List<GroupCommentResponseDto> commentDtos =
                    groupCommentMapper.toResponseDtoList(
                            activeComments
                    );

            return GroupCommentsVisibilityResponseDto.builder()
                    .comments(commentDtos)
                    .totalActiveComments(activeComments.size())
                    .visibleCount(activeComments.size())
                    .hiddenByProgressCount(0)
                    .build();
        }

        validateActiveMembership(
                groupId,
                authenticatedUser
        );

        validateActiveParticipation(
                activeCycle.getIdExternal(),
                authenticatedUser
        );

        ReadingStatusEntity readingStatus =
                findReadingStatusForCycle(
                        activeCycle,
                        authenticatedUser
                );

        validateReadingStatus(readingStatus);

        double currentProgressPercentage =
                normalizeToPercentage(
                        readingStatus.getProgressType(),
                        readingStatus.getProgressValue(),
                        activeCycle
                );

        List<GroupCommentEntity> activeComments =
                findActiveCommentsForCycle(activeCycle);

        List<GroupCommentEntity> visibleComments =
                activeComments.stream()
                        .filter(comment ->
                                isCommentVisible(
                                        comment,
                                        currentProgressPercentage,
                                        activeCycle
                                )
                        )
                        .toList();

        int totalActiveComments =
                activeComments.size();

        int visibleCount =
                visibleComments.size();

        int hiddenByProgressCount =
                totalActiveComments - visibleCount;

        return GroupCommentsVisibilityResponseDto.builder()
                .comments(
                        groupCommentMapper.toResponseDtoList(
                                visibleComments
                        )
                )
                .totalActiveComments(totalActiveComments)
                .visibleCount(visibleCount)
                .hiddenByProgressCount(hiddenByProgressCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupCommentResponseDto> getActiveCommentsByUserId(
            UUID userId
    ) {
        UserEntity authenticatedUser =
                getAuthenticatedUser();

        findUserByIdExternal(userId);

        boolean isSameUser =
                Objects.equals(
                        userId,
                        authenticatedUser.getIdExternal()
                );

        boolean isAdminOrModerator =
                isAdminOrModerator(authenticatedUser);

        if (!isSameUser && !isAdminOrModerator) {
            throw new AccessDeniedException(
                    "You do not have permission to view this user's comments"
            );
        }

        List<GroupCommentEntity> comments =
                groupCommentRepository
                        .findByUserIdExternalAndStatusOrderByCreatedAtDesc(
                                userId,
                                GroupCommentStatus.ACTIVE
                        );

        return groupCommentMapper.toResponseDtoList(comments);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupCommentResponseDto getCommentByIdExternal(
            UUID commentId
    ) {
        GroupCommentEntity comment =
                findCommentByIdExternal(commentId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        boolean isAuthor =
                Objects.equals(
                        comment.getUser().getIdExternal(),
                        authenticatedUser.getIdExternal()
                );

        boolean isAdminOrModerator =
                isAdminOrModerator(authenticatedUser);

        if (comment.getStatus() != GroupCommentStatus.ACTIVE
                && !isAuthor
                && !isAdminOrModerator) {
            throw new AccessDeniedException(
                    "You do not have permission to view this comment"
            );
        }

        if (isAuthor || isAdminOrModerator) {
            return groupCommentMapper.toResponseDto(comment);
        }

        UUID groupId =
                comment.getGroup().getIdExternal();

        GroupReadingCycleEntity commentCycle =
                comment.getReadingCycle();

        validateActiveMembership(
                groupId,
                authenticatedUser
        );

        validateActiveParticipation(
                commentCycle.getIdExternal(),
                authenticatedUser
        );

        ReadingStatusEntity readingStatus =
                findReadingStatusForCycle(
                        commentCycle,
                        authenticatedUser
                );

        validateReadingStatus(readingStatus);

        double currentProgressPercentage =
                normalizeToPercentage(
                        readingStatus.getProgressType(),
                        readingStatus.getProgressValue(),
                        commentCycle
                );

        double commentProgressPercentage =
                normalizeToPercentage(
                        comment.getProgressType(),
                        comment.getProgressValue(),
                        commentCycle
                );

        if (commentProgressPercentage > currentProgressPercentage) {
            throw new AccessDeniedException(
                    "You do not have permission to view this comment"
            );
        }

        return groupCommentMapper.toResponseDto(comment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        GroupCommentEntity comment =
                findCommentByIdExternal(commentId);

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        boolean isAuthor =
                Objects.equals(
                        comment.getUser().getIdExternal(),
                        authenticatedUser.getIdExternal()
                );

        boolean isAdminOrModerator =
                isAdminOrModerator(authenticatedUser);

        if (!isAuthor && !isAdminOrModerator) {
            throw new AccessDeniedException(
                    "You do not have permission to delete this comment"
            );
        }

        if (comment.getStatus() != GroupCommentStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Only active comments can be deleted"
            );
        }

        if (isAdminOrModerator && !isAuthor) {
            comment.setStatus(GroupCommentStatus.MODERATED);
        } else {
            comment.setStatus(GroupCommentStatus.DELETED);
        }

        groupCommentRepository.save(comment);
    }

    private List<GroupCommentEntity> findActiveCommentsForCycle(
            GroupReadingCycleEntity cycle
    ) {
        return groupCommentRepository
                .findByReadingCycle_IdExternalAndStatusOrderByCreatedAtAsc(
                        cycle.getIdExternal(),
                        GroupCommentStatus.ACTIVE
                );
    }

    private boolean isCommentVisible(
            GroupCommentEntity comment,
            double currentProgressPercentage,
            GroupReadingCycleEntity cycle
    ) {
        double commentProgressPercentage =
                normalizeToPercentage(
                        comment.getProgressType(),
                        comment.getProgressValue(),
                        cycle
                );

        return commentProgressPercentage
                <= currentProgressPercentage;
    }

    private void validateActiveMembership(
            UUID groupId,
            UserEntity authenticatedUser
    ) {
        boolean isActiveMember =
                groupMemberRepository
                        .existsByGroupIdExternalAndUserIdExternalAndStatus(
                                groupId,
                                authenticatedUser.getIdExternal(),
                                GroupMemberStatus.ACTIVE
                        );

        if (!isActiveMember) {
            throw new AccessDeniedException(
                    "Only active group members can perform this action"
            );
        }
    }

    private void validateActiveParticipation(
            UUID cycleId,
            UserEntity authenticatedUser
    ) {
        GroupReadingParticipantEntity participant =
                participantRepository
                        .findByReadingCycle_IdExternalAndGroupMember_User_IdExternal(
                                cycleId,
                                authenticatedUser.getIdExternal()
                        )
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Only participants of the reading cycle can perform this action"
                                )
                        );

        if (!VALID_PARTICIPANT_STATUSES.contains(
                participant.getStatus()
        )) {
            throw new AccessDeniedException(
                    "Only active or completed reading cycle participants can perform this action"
            );
        }
    }

    private ReadingStatusEntity findReadingStatusForCycle(
            GroupReadingCycleEntity cycle,
            UserEntity user
    ) {
        if (cycle.getBook() != null) {
            return readingStatusRepository
                    .findByUserIdExternalAndBookIdExternal(
                            user.getIdExternal(),
                            cycle.getBook().getIdExternal()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Reading status not found for the authenticated user and the reading cycle's book"
                            )
                    );
        }

        if (cycle.getStory() != null) {
            return readingStatusRepository
                    .findByUserIdExternalAndStoryIdExternal(
                            user.getIdExternal(),
                            cycle.getStory().getIdExternal()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Reading status not found for the authenticated user and the reading cycle's story"
                            )
                    );
        }

        throw new BadRequestException(
                "Reading cycle must be associated with either a book or a story"
        );
    }

    private void validateReadingStatus(
            ReadingStatusEntity readingStatus
    ) {
        if (!VALID_READING_STATUSES.contains(
                readingStatus.getStatus()
        )) {
            throw new BadRequestException(
                    "Reading status must be IN_PROGRESS, RE_READING, or FINISHED to interact with group comments"
            );
        }

        if (readingStatus.getProgressType() == null
                || readingStatus.getProgressValue() == null) {
            throw new BadRequestException(
                    "Reading progress is required to interact with group comments"
            );
        }
    }

    private void validateProgressReference(
            ProgressType progressType,
            Integer progressValue,
            GroupReadingCycleEntity cycle
    ) {
        if (progressType == null || progressValue == null) {
            throw new BadRequestException(
                    "Progress type and progress value are required"
            );
        }

        if (progressValue <= 0) {
            throw new BadRequestException(
                    "Progress value must be greater than 0"
            );
        }

        if (progressType == ProgressType.PERCENTAGE
                && progressValue > 100) {
            throw new BadRequestException(
                    "Percentage progress cannot be greater than 100"
            );
        }

        if (progressType == ProgressType.CHAPTER) {
            int totalChapters =
                    getCycleTotalChapters(cycle);

            if (progressValue > totalChapters) {
                throw new BadRequestException(
                        "Chapter progress cannot be greater than the reading cycle's total chapters"
                );
            }
        }
    }

    private double normalizeToPercentage(
            ProgressType progressType,
            Integer progressValue,
            GroupReadingCycleEntity cycle
    ) {
        if (progressType == null || progressValue == null) {
            throw new BadRequestException(
                    "Progress type and progress value are required"
            );
        }

        if (progressType == ProgressType.PERCENTAGE) {
            return Math.max(
                    0.0,
                    Math.min(
                            progressValue.doubleValue(),
                            100.0
                    )
            );
        }

        if (progressType == ProgressType.CHAPTER) {
            int totalChapters =
                    getCycleTotalChapters(cycle);

            double percentage =
                    progressValue * 100.0
                            / totalChapters;

            return Math.max(
                    0.0,
                    Math.min(
                            percentage,
                            100.0
                    )
            );
        }

        throw new BadRequestException(
                "Unsupported progress type"
        );
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
                throw new BadRequestException(
                        "Story reading cycle does not have a valid chapter reference"
                );
            }

            int totalChapters =
                    (int) cycle.getStory()
                            .getChapters()
                            .stream()
                            .filter(chapter ->
                                    chapter.isPublished()
                                            && !chapter.isDeleted()
                            )
                            .count();

            if (totalChapters <= 0) {
                throw new BadRequestException(
                        "Story reading cycle does not have published chapters"
                );
            }

            return totalChapters;
        }

        throw new BadRequestException(
                "Reading cycle must be associated with either a book or a story"
        );
    }

    private boolean isAdminOrModerator(UserEntity user) {
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

    private GroupCommentEntity findCommentByIdExternal(
            UUID commentId
    ) {
        return groupCommentRepository
                .findByIdExternal(commentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Group comment not found with idExternal: "
                                        + commentId
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

    private GroupReadingCycleEntity findActiveCycle(
            UUID groupId
    ) {
        return groupReadingCycleRepository
                .findByGroup_IdExternalAndStatus(
                        groupId,
                        GroupReadingCycleStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Active reading cycle not found for group: "
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