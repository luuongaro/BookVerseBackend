package com.grupo3.BookVerse.features.groups.groupComment.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentEntity;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentStatus;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentRequestDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentResponseDto;
import com.grupo3.BookVerse.features.groups.groupComment.mappers.GroupCommentMapper;
import com.grupo3.BookVerse.features.groups.groupComment.repository.GroupCommentRepository;
import com.grupo3.BookVerse.features.groups.groupComment.service.GroupCommentService;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.status.domain.ProgressType;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupCommentServiceImpl implements GroupCommentService {

    private final GroupCommentRepository groupCommentRepository;
    private final GroupCommentMapper groupCommentMapper;
    private final ReadingGroupRepository readingGroupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ReadingStatusRepository readingStatusRepository;
    private final GroupReadingCycleRepository groupReadingCycleRepository;

    @Override
    @Transactional
    public GroupCommentResponseDto createComment(UUID groupId, GroupCommentRequestDto requestDto) {
        ReadingGroupEntity group = findGroupByIdExternal(groupId);
        GroupReadingCycleEntity activeCycle = findActiveCycle(groupId);

        validateBookOnlyCycle(activeCycle);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateActiveMembership(groupId, authenticatedUser);

        Integer currentProgress =
                getCurrentBookProgressPercent(
                        activeCycle,
                        authenticatedUser
                );

        if (requestDto.getProgressPercent() > currentProgress) {
            throw new BadRequestException("You cannot comment beyond your current reading progress");
        }

        GroupCommentEntity comment = groupCommentMapper.toEntity(requestDto);
        comment.setGroup(group);
        comment.setUser(authenticatedUser);
        comment.setStatus(GroupCommentStatus.ACTIVE);

        GroupCommentEntity savedComment = groupCommentRepository.save(comment);
        return groupCommentMapper.toResponseDto(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupCommentResponseDto> getVisibleCommentsByGroupId(UUID groupId) {
        findGroupByIdExternal(groupId);

        GroupReadingCycleEntity activeCycle =
                findActiveCycle(groupId);

        validateBookOnlyCycle(activeCycle);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateActiveMembership(groupId, authenticatedUser);

        Integer currentProgress =
                getCurrentBookProgressPercent(
                        activeCycle,
                        authenticatedUser
                );

        List<GroupCommentEntity> visibleComments =
                groupCommentRepository.findByGroupIdExternalAndStatusAndProgressPercentLessThanEqualOrderByCreatedAtAsc(
                        groupId,
                        GroupCommentStatus.ACTIVE,
                        currentProgress
                );

        return groupCommentMapper.toResponseDtoList(visibleComments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupCommentResponseDto> getActiveCommentsByUserId(UUID userId) {
        UserEntity authenticatedUser = getAuthenticatedUser();
        findUserByIdExternal(userId);

        boolean isSameUser = Objects.equals(userId, authenticatedUser.getIdExternal());
        boolean isAdminOrModerator = isAdminOrModerator(authenticatedUser);

        if (!isSameUser && !isAdminOrModerator) {
            throw new AccessDeniedException("You do not have permission to view this user's comments");
        }

        List<GroupCommentEntity> comments =
                groupCommentRepository.findByUserIdExternalAndStatusOrderByCreatedAtDesc(
                        userId,
                        GroupCommentStatus.ACTIVE
                );

        return groupCommentMapper.toResponseDtoList(comments);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupCommentResponseDto getCommentByIdExternal(UUID commentId) {
        GroupCommentEntity comment = findCommentByIdExternal(commentId);
        UserEntity authenticatedUser = getAuthenticatedUser();

        boolean isAuthor = Objects.equals(
                comment.getUser().getIdExternal(),
                authenticatedUser.getIdExternal()
        );

        boolean isAdminOrModerator = isAdminOrModerator(authenticatedUser);

        boolean isActiveMember = groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndStatus(
                        comment.getGroup().getIdExternal(),
                        authenticatedUser.getIdExternal(),
                        GroupMemberStatus.ACTIVE
                );

        if (!isAuthor && !isAdminOrModerator && !isActiveMember) {
            throw new AccessDeniedException("You do not have permission to view this comment");
        }

        if (comment.getStatus() != GroupCommentStatus.ACTIVE && !isAuthor && !isAdminOrModerator) {
            throw new AccessDeniedException("You do not have permission to view this comment");
        }

        if (!isAuthor && !isAdminOrModerator) {
            GroupReadingCycleEntity activeCycle =
                    findActiveCycle(
                            comment.getGroup().getIdExternal()
                    );

            validateBookOnlyCycle(activeCycle);

            Integer currentProgress =
                    getCurrentBookProgressPercent(
                            activeCycle,
                            authenticatedUser
                    );

            if (comment.getProgressPercent() > currentProgress) {
                throw new AccessDeniedException("You do not have permission to view this comment");
            }
        }

        return groupCommentMapper.toResponseDto(comment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        GroupCommentEntity comment = findCommentByIdExternal(commentId);
        UserEntity authenticatedUser = getAuthenticatedUser();

        boolean isAuthor = Objects.equals(
                comment.getUser().getIdExternal(),
                authenticatedUser.getIdExternal()
        );

        boolean isAdminOrModerator = isAdminOrModerator(authenticatedUser);

        if (!isAuthor && !isAdminOrModerator) {
            throw new AccessDeniedException("You do not have permission to delete this comment");
        }

        if (comment.getStatus() != GroupCommentStatus.ACTIVE) {
            throw new AccessDeniedException("Only active comments can be deleted");
        }

        if (isAdminOrModerator && !isAuthor) {
            comment.setStatus(GroupCommentStatus.MODERATED);
        } else {
            comment.setStatus(GroupCommentStatus.DELETED);
        }

        groupCommentRepository.save(comment);
    }

    private void validateActiveMembership(UUID groupId, UserEntity authenticatedUser) {
        boolean isActiveMember = groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndStatus(
                        groupId,
                        authenticatedUser.getIdExternal(),
                        GroupMemberStatus.ACTIVE
                );

        if (!isActiveMember && !isAdminOrModerator(authenticatedUser)) {
            throw new AccessDeniedException("Only active group members can perform this action");
        }
    }

    private void validateBookOnlyCycle(
            GroupReadingCycleEntity cycle
    ) {
        if (cycle.getBook() == null
                || cycle.getStory() != null) {
            throw new BadRequestException(
                    "Group comments are currently supported only for book-based reading cycles"
            );
        }
    }

    private Integer getCurrentBookProgressPercent(
            GroupReadingCycleEntity cycle,
            UserEntity user
    ) {
        ReadingStatusEntity readingStatus =
                readingStatusRepository
                        .findByUserIdExternalAndBookIdExternal(
                                user.getIdExternal(),
                                cycle.getBook().getIdExternal()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reading status not found for the authenticated user and the active cycle's book"
                                )
                        );

        if (readingStatus.getProgressType()
                != ProgressType.PERCENTAGE
                || readingStatus.getProgressValue() == null) {
            throw new BadRequestException(
                    "A percentage-based reading status is required to interact with book group comments"
            );
        }

        return readingStatus.getProgressValue();
    }

    private boolean isAdminOrModerator(UserEntity user) {
        return user.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority())
                                || "ROLE_MODERATOR".equals(authority.getAuthority())
                );
    }

    private GroupCommentEntity findCommentByIdExternal(UUID commentId) {
        return groupCommentRepository.findByIdExternal(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group comment not found with idExternal: " + commentId
                ));
    }

    private ReadingGroupEntity findGroupByIdExternal(UUID groupId) {
        return readingGroupRepository.findByIdExternal(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading group not found with idExternal: " + groupId
                ));
    }

    private UserEntity findUserByIdExternal(UUID userId) {
        return userRepository.findByIdExternal(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with idExternal: " + userId
                ));
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }
}

