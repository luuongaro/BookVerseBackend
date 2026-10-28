package com.grupo3.BookVerse.features.reviewReport.services.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.reviewReport.domain.ReviewReportEntity;
import com.grupo3.BookVerse.features.reviewReport.domain.ReviewReportStatus;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportCreateRequestDto;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportModerationRequestDto;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportResponseDto;
import com.grupo3.BookVerse.features.reviewReport.mapper.ReviewReportMapper;
import com.grupo3.BookVerse.features.reviewReport.repository.ReviewReportRepository;
import com.grupo3.BookVerse.features.reviewReport.services.ReviewReportService;
import com.grupo3.BookVerse.features.reviews.domain.ReviewEntity;
import com.grupo3.BookVerse.features.reviews.domain.ReviewTakedownStatus;
import com.grupo3.BookVerse.features.reviews.repository.ReviewRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewReportServiceImpl implements ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewReportMapper reviewReportMapper;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewReportResponseDto> getAllReports() {
        List<ReviewReportEntity> reports =
                reviewReportRepository.findAllByOrderByCreatedAtDesc();

        return reviewReportMapper.toResponseListDto(reports);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewReportResponseDto> getPendingReports() {
        List<ReviewReportEntity> reports =
                reviewReportRepository.findByStatusOrderByCreatedAtDesc(
                        ReviewReportStatus.PENDING
                );

        return reviewReportMapper.toResponseListDto(reports);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewReportResponseDto getReportById(UUID reportId) {
        ReviewReportEntity report = findReportByIdExternal(reportId);
        return reviewReportMapper.toResponseDto(report);
    }

    @Override
    @Transactional
    public ReviewReportResponseDto createReport(ReviewReportCreateRequestDto dto) {
        UserEntity reporter = getAuthenticatedUser();

        ReviewEntity review = findActiveReviewByIdExternal(dto.reviewId());

        validateReviewCanBeReported(review);
        validateReporterIsNotReviewOwner(review, reporter);
        validateNoDuplicatePendingReport(review.getId(), reporter.getId());

        ReviewReportEntity report = reviewReportMapper.toEntity(dto);
        report.setReview(review);
        report.setReporterUser(reporter);
        report.setStatus(ReviewReportStatus.PENDING);

        ReviewReportEntity saved = reviewReportRepository.save(report);
        return reviewReportMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public ReviewReportResponseDto approveReport(
            UUID reportId,
            ReviewReportModerationRequestDto dto
    ) {
        ReviewReportEntity report = findPendingReportByIdExternal(reportId);
        UserEntity moderator = getAuthenticatedUser();

        report.setModeratorUser(moderator);
        report.setStatus(ReviewReportStatus.APPROVED);
        report.setResolutionComment(dto.resolutionComment());
        report.setResolvedAt(LocalDateTime.now());

        ReviewEntity review = report.getReview();
        review.setTakedownStatus(ReviewTakedownStatus.TAKEN_DOWN);
        review.setTakedownAt(LocalDateTime.now());
        review.setTakedownReason(
                dto.resolutionComment() != null
                        && !dto.resolutionComment().isBlank()
                        ? dto.resolutionComment()
                        : report.getReason()
        );

        reviewRepository.save(review);

        ReviewReportEntity updated =
                reviewReportRepository.save(report);

        return reviewReportMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public ReviewReportResponseDto rejectReport(
            UUID reportId,
            ReviewReportModerationRequestDto dto
    ) {
        ReviewReportEntity report = findPendingReportByIdExternal(reportId);
        UserEntity moderator = getAuthenticatedUser();

        report.setModeratorUser(moderator);
        report.setStatus(ReviewReportStatus.REJECTED);
        report.setResolutionComment(dto.resolutionComment());
        report.setResolvedAt(LocalDateTime.now());

        ReviewReportEntity updated = reviewReportRepository.save(report);
        return reviewReportMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void restoreReview(UUID reportId) {
        ReviewReportEntity report = findReportByIdExternal(reportId);

        validateReportAllowsRestoration(report);

        ReviewEntity review = report.getReview();

        review.setTakedownStatus(ReviewTakedownStatus.ACTIVE);
        review.setTakedownAt(null);
        review.setTakedownReason(null);

        reviewRepository.save(review);
    }

    private ReviewReportEntity findReportByIdExternal(UUID reportId) {
        return reviewReportRepository.findByIdExternal(reportId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Review report not found with idExternal: " + reportId
                        )
                );
    }

    private ReviewReportEntity findPendingReportByIdExternal(UUID reportId) {
        ReviewReportEntity report = findReportByIdExternal(reportId);

        if (report.getStatus() != ReviewReportStatus.PENDING) {
            throw new BadRequestException(
                    "Only pending reports can be moderated"
            );
        }

        return report;
    }

    private void validateReportAllowsRestoration(
            ReviewReportEntity report
    ) {
        if (report.getStatus() != ReviewReportStatus.APPROVED) {
            throw new BadRequestException(
                    "Only an approved review report can restore its associated review"
            );
        }

        ReviewEntity review = report.getReview();

        if (review == null) {
            throw new ResourceNotFoundException(
                    "The review associated with this report was not found"
            );
        }

        if (review.isDeleted()) {
            throw new BadRequestException(
                    "A deleted review cannot be restored"
            );
        }

        if (review.getTakedownStatus() != ReviewTakedownStatus.TAKEN_DOWN) {
            throw new BadRequestException(
                    "The review is not currently taken down"
            );
        }
    }
    private ReviewEntity findActiveReviewByIdExternal(UUID reviewId) {
        return reviewRepository.findByIdExternalAndDeletedFalse(reviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Review not found with idExternal: " + reviewId
                        )
                );
    }


    private UserEntity getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }

    private void validateReviewCanBeReported(ReviewEntity review) {
        if (review.getTakedownStatus() == ReviewTakedownStatus.TAKEN_DOWN) {
            throw new BadRequestException(
                    "A taken down review cannot be reported again"
            );
        }
    }

    private void validateReporterIsNotReviewOwner(
            ReviewEntity review,
            UserEntity reporter
    ) {
        if (review.getReviewer() != null
                && review.getReviewer().getId().equals(reporter.getId())) {
            throw new BadRequestException(
                    "Users cannot report their own reviews"
            );
        }
    }

    private void validateNoDuplicatePendingReport(
            Long reviewId,
            Long reporterUserId
    ) {
        boolean alreadyReported =
                reviewReportRepository.existsByReviewIdAndReporterUserIdAndStatus(
                        reviewId,
                        reporterUserId,
                        ReviewReportStatus.PENDING
                );

        if (alreadyReported) {
            throw new DuplicateResourceException(
                    "There is already a pending report for this review by the same user"
            );
        }
    }


}