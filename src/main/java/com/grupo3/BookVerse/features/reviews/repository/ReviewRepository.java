package com.grupo3.BookVerse.features.reviews.repository;

import com.grupo3.BookVerse.features.reviews.domain.ReviewEntity;
import com.grupo3.BookVerse.features.reviews.domain.ReviewTakedownStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    Optional<ReviewEntity> findByIdExternalAndDeletedFalse(UUID idExternal);

    List<ReviewEntity> findAllByDeletedFalseOrderByCreatedAtDesc();

    List<ReviewEntity> findByBookIdAndDeletedFalseOrderByCreatedAtDesc(Long bookId);

    List<ReviewEntity> findByStoryIdAndDeletedFalseOrderByCreatedAtDesc(Long storyId);

    Optional<ReviewEntity> findByIdExternalAndDeletedFalseAndTakedownStatus(
            UUID idExternal,
            ReviewTakedownStatus takedownStatus
    );

    List<ReviewEntity> findAllByDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
            ReviewTakedownStatus takedownStatus
    );

    List<ReviewEntity> findByBookIdAndDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
            Long bookId,
            ReviewTakedownStatus takedownStatus
    );

    List<ReviewEntity> findByStoryIdAndDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
            Long storyId,
            ReviewTakedownStatus takedownStatus
    );

    boolean existsByReviewerIdAndBookIdAndDeletedFalse(Long reviewerId, Long bookId);

    boolean existsByReviewerIdAndStoryIdAndDeletedFalse(Long reviewerId, Long storyId);
}