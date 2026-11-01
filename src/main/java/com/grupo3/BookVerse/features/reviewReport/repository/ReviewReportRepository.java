package com.grupo3.BookVerse.features.reviewReport.repository;

import com.grupo3.BookVerse.features.reviewReport.domain.ReviewReportEntity;
import com.grupo3.BookVerse.features.reviewReport.domain.ReviewReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReportEntity, Long> {

    Optional<ReviewReportEntity> findByIdExternal(UUID idExternal);

    List<ReviewReportEntity> findAllByOrderByCreatedAtDesc();

    List<ReviewReportEntity> findByStatusOrderByCreatedAtDesc(ReviewReportStatus status);

    List<ReviewReportEntity> findAllByReviewIdAndStatus(
            Long reviewId,
            ReviewReportStatus status
    );

    boolean existsByReviewIdAndReporterUserIdAndStatus(
            Long reviewId,
            Long reporterUserId,
            ReviewReportStatus status
    );
}