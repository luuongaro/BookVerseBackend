package com.grupo3.BookVerse.features.groups.groupCommentReport.repository;

import com.grupo3.BookVerse.features.groups.groupCommentReport.domain.GroupCommentReportEntity;
import com.grupo3.BookVerse.features.groups.groupCommentReport.domain.GroupCommentReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupCommentReportRepository
        extends JpaRepository<GroupCommentReportEntity, Long> {

    Optional<GroupCommentReportEntity> findByIdExternal(
            UUID idExternal
    );

    List<GroupCommentReportEntity>
    findAllByOrderByCreatedAtDesc();

    List<GroupCommentReportEntity>
    findByStatusOrderByCreatedAtDesc(
            GroupCommentReportStatus status
    );

    List<GroupCommentReportEntity>
    findAllByCommentIdAndStatus(
            Long commentId,
            GroupCommentReportStatus status
    );

    boolean existsByCommentIdAndReporterUserIdAndStatus(
            Long commentId,
            Long reporterUserId,
            GroupCommentReportStatus status
    );
}