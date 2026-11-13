package com.grupo3.BookVerse.features.groups.groupCommentReport.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentEntity;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentStatus;
import com.grupo3.BookVerse.features.groups.groupComment.repository.GroupCommentRepository;
import com.grupo3.BookVerse.features.groups.groupCommentReport.domain.GroupCommentReportEntity;
import com.grupo3.BookVerse.features.groups.groupCommentReport.domain.GroupCommentReportStatus;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportCreateRequestDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportModerationRequestDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportResponseDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.mapper.GroupCommentReportMapper;
import com.grupo3.BookVerse.features.groups.groupCommentReport.repository.GroupCommentReportRepository;
import com.grupo3.BookVerse.features.groups.groupCommentReport.service.GroupCommentReportService;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupCommentReportServiceImpl
        implements GroupCommentReportService {

    private final GroupCommentReportRepository reportRepository;
    private final GroupCommentReportMapper reportMapper;
    private final GroupCommentRepository commentRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GroupCommentReportResponseDto> getAllReports() {

        List<GroupCommentReportEntity> reports =
                reportRepository.findAllByOrderByCreatedAtDesc();

        return reportMapper.toResponseDtoList(reports);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupCommentReportResponseDto> getPendingReports() {

        List<GroupCommentReportEntity> reports =
                reportRepository.findByStatusOrderByCreatedAtDesc(
                        GroupCommentReportStatus.PENDING
                );

        return reportMapper.toResponseDtoList(reports);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupCommentReportResponseDto getReportById(
            UUID reportId
    ) {
        GroupCommentReportEntity report =
                findReportByIdExternal(reportId);

        return reportMapper.toResponseDto(report);
    }

    @Override
    @Transactional
    public GroupCommentReportResponseDto createReport(
            GroupCommentReportCreateRequestDto dto
    ) {
        UserEntity reporter =
                getAuthenticatedUser();

        GroupCommentEntity comment =
                findCommentByIdExternal(dto.commentId());

        validateCommentCanBeReported(comment);

        validateReporterIsActiveGroupMember(
                comment,
                reporter
        );

        validateReporterIsNotCommentAuthor(
                comment,
                reporter
        );

        validateNoDuplicatePendingReport(
                comment.getId(),
                reporter.getId()
        );

        GroupCommentReportEntity report =
                reportMapper.toEntity(dto);

        report.setComment(comment);
        report.setReporterUser(reporter);
        report.setStatus(
                GroupCommentReportStatus.PENDING
        );

        GroupCommentReportEntity saved =
                reportRepository.save(report);

        return reportMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public GroupCommentReportResponseDto approveReport(
            UUID reportId,
            GroupCommentReportModerationRequestDto dto
    ) {
        GroupCommentReportEntity selectedReport =
                findPendingReportByIdExternal(reportId);

        UserEntity moderator =
                getAuthenticatedUser();

        GroupCommentEntity comment =
                selectedReport.getComment();

        if (comment == null) {
            throw new ResourceNotFoundException(
                    "The comment associated with this report was not found"
            );
        }

        if (comment.getStatus() != GroupCommentStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only active comments can be moderated"
            );
        }

        comment.setStatus(
                GroupCommentStatus.MODERATED
        );

        commentRepository.save(comment);

        String resolutionComment =
                dto == null
                        ? null
                        : dto.resolutionComment();

        resolvePendingReportsForComment(
                comment.getId(),
                moderator,
                GroupCommentReportStatus.APPROVED,
                resolutionComment
        );

        return reportMapper.toResponseDto(
                selectedReport
        );
    }

    @Override
    @Transactional
    public GroupCommentReportResponseDto rejectReport(
            UUID reportId,
            GroupCommentReportModerationRequestDto dto
    ) {
        GroupCommentReportEntity selectedReport =
                findPendingReportByIdExternal(reportId);

        UserEntity moderator =
                getAuthenticatedUser();

        String resolutionComment =
                dto == null
                        ? null
                        : dto.resolutionComment();

        resolvePendingReportsForComment(
                selectedReport.getComment().getId(),
                moderator,
                GroupCommentReportStatus.REJECTED,
                resolutionComment
        );

        return reportMapper.toResponseDto(
                selectedReport
        );
    }

    private void resolvePendingReportsForComment(
            Long commentId,
            UserEntity moderator,
            GroupCommentReportStatus resolutionStatus,
            String resolutionComment
    ) {
        List<GroupCommentReportEntity> pendingReports =
                reportRepository
                        .findAllByCommentIdAndStatus(
                                commentId,
                                GroupCommentReportStatus.PENDING
                        );

        LocalDateTime resolvedAt =
                LocalDateTime.now();

        for (GroupCommentReportEntity pendingReport
                : pendingReports) {

            pendingReport.setModeratorUser(
                    moderator
            );

            pendingReport.setStatus(
                    resolutionStatus
            );

            pendingReport.setResolutionComment(
                    resolutionComment
            );

            pendingReport.setResolvedAt(
                    resolvedAt
            );
        }

        reportRepository.saveAll(
                pendingReports
        );
    }

    private GroupCommentReportEntity findReportByIdExternal(
            UUID reportId
    ) {
        return reportRepository
                .findByIdExternal(reportId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Group comment report not found with idExternal: "
                                        + reportId
                        )
                );
    }

    private GroupCommentReportEntity findPendingReportByIdExternal(
            UUID reportId
    ) {
        GroupCommentReportEntity report =
                findReportByIdExternal(reportId);

        if (report.getStatus()
                != GroupCommentReportStatus.PENDING) {
            throw new BadRequestException(
                    "Only pending group comment reports can be moderated"
            );
        }

        return report;
    }

    private GroupCommentEntity findCommentByIdExternal(
            UUID commentId
    ) {
        return commentRepository
                .findByIdExternal(commentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Group comment not found with idExternal: "
                                        + commentId
                        )
                );
    }

    private void validateCommentCanBeReported(
            GroupCommentEntity comment
    ) {
        if (comment.getStatus()
                != GroupCommentStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only active comments can be reported"
            );
        }

        if (comment.getGroup() == null) {
            throw new ResourceNotFoundException(
                    "The group associated with this comment was not found"
            );
        }
    }

    private void validateReporterIsActiveGroupMember(
            GroupCommentEntity comment,
            UserEntity reporter
    ) {
        boolean isActiveMember =
                groupMemberRepository
                        .existsByGroupIdExternalAndUserIdExternalAndStatus(
                                comment.getGroup().getIdExternal(),
                                reporter.getIdExternal(),
                                GroupMemberStatus.ACTIVE
                        );

        if (!isActiveMember
                && !isAdminOrModerator(reporter)) {
            throw new AccessDeniedException(
                    "Only active group members can report group comments"
            );
        }
    }

    private void validateReporterIsNotCommentAuthor(
            GroupCommentEntity comment,
            UserEntity reporter
    ) {
        boolean isAuthor =
                comment.getUser() != null
                        && Objects.equals(
                        comment.getUser().getId(),
                        reporter.getId()
                );

        if (isAuthor) {
            throw new BadRequestException(
                    "Users cannot report their own comments"
            );
        }
    }

    private void validateNoDuplicatePendingReport(
            Long commentId,
            Long reporterUserId
    ) {
        boolean alreadyReported =
                reportRepository
                        .existsByCommentIdAndReporterUserIdAndStatus(
                                commentId,
                                reporterUserId,
                                GroupCommentReportStatus.PENDING
                        );

        if (alreadyReported) {
            throw new DuplicateResourceException(
                    "There is already a pending report for this comment by the same user"
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