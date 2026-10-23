package com.grupo3.BookVerse.features.reviews.services.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.books.repository.BookRepository;
import com.grupo3.BookVerse.features.reviews.domain.ReviewEntity;
import com.grupo3.BookVerse.features.reviews.domain.ReviewTakedownStatus;
import com.grupo3.BookVerse.features.reviews.dto.ReviewCreateRequestDto;
import com.grupo3.BookVerse.features.reviews.dto.ReviewResponseDto;
import com.grupo3.BookVerse.features.reviews.dto.ReviewUpdateRequestDto;
import com.grupo3.BookVerse.features.reviews.mapper.ReviewMapper;
import com.grupo3.BookVerse.features.reviews.repository.ReviewRepository;
import com.grupo3.BookVerse.features.reviews.services.ReviewService;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final StoryRepository storyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllReviews() {
        List<ReviewEntity> reviews =
                reviewRepository.findAllByDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
                        ReviewTakedownStatus.ACTIVE
                );

        return reviewMapper.toResponseListDto(reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponseDto getReviewById(UUID reviewId) {
        ReviewEntity review = findActiveReviewByIdExternal(reviewId);
        return reviewMapper.toResponseDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByBookId(UUID bookId) {
        BookEntity book = findActiveBookByIdExternal(bookId);

        List<ReviewEntity> reviews =
                reviewRepository.findByBookIdAndDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
                        book.getId(),
                        ReviewTakedownStatus.ACTIVE
                );

        return reviewMapper.toResponseListDto(reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsByStoryId(UUID storyId) {
        StoryEntity story = findActiveStoryByIdExternal(storyId);

        List<ReviewEntity> reviews =
                reviewRepository.findByStoryIdAndDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
                        story.getId(),
                        ReviewTakedownStatus.ACTIVE
                );

        return reviewMapper.toResponseListDto(reviews);
    }

    @Override
    @Transactional
    public ReviewResponseDto createReview(ReviewCreateRequestDto dto) {
        validateExactlyOneTarget(dto.bookId(), dto.storyId());

        UserEntity authenticatedUser = getAuthenticatedUser();
        UserEntity reviewer = findUserByIdExternal(dto.reviewerId());

        validateAuthenticatedUserMatchesReviewer(authenticatedUser, reviewer);

        ReviewEntity review = reviewMapper.toEntity(dto);
        review.setReviewer(reviewer);
        review.setTakedownStatus(ReviewTakedownStatus.ACTIVE);

        if (dto.bookId() != null) {
            BookEntity book = findActiveBookByIdExternal(dto.bookId());

            validateDuplicateBookReview(reviewer.getId(), book.getId());

            review.setBook(book);
            review.setStory(null);
        }

        if (dto.storyId() != null) {
            StoryEntity story = findActiveStoryByIdExternal(dto.storyId());

            validateStoryIsNotOwnedByReviewer(story, reviewer);
            validateDuplicateStoryReview(reviewer.getId(), story.getId());

            review.setStory(story);
            review.setBook(null);
        }

        ReviewEntity saved = reviewRepository.save(review);
        return reviewMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public ReviewResponseDto updateReview(UUID reviewId, ReviewUpdateRequestDto dto) {
        ReviewEntity existing = findActiveReviewByIdExternal(reviewId);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanManageReview(existing, authenticatedUser);

        existing.setRating(dto.rating());
        existing.setContent(dto.content());

        ReviewEntity updated = reviewRepository.save(existing);
        return reviewMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteReview(UUID reviewId) {
        ReviewEntity review = findActiveReviewByIdExternal(reviewId);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanManageReview(review, authenticatedUser);

        review.setDeleted(true);
        reviewRepository.save(review);
    }

    private ReviewEntity findActiveReviewByIdExternal(UUID reviewId) {
        return reviewRepository.findByIdExternalAndDeletedFalseAndTakedownStatus(
                        reviewId,
                        ReviewTakedownStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Review not found with id: " + reviewId
                        )
                );
    }

    private UserEntity findUserByIdExternal(UUID userId) {
        return userRepository.findByIdExternal(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with idExternal: " + userId
                        )
                );
    }

    private BookEntity findActiveBookByIdExternal(UUID bookId) {
        BookEntity book = bookRepository.findByIdExternal(bookId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with idExternal: " + bookId
                        )
                );

        if (Boolean.TRUE.equals(book.getDeleted())) {
            throw new ResourceNotFoundException(
                    "Book not found with idExternal: " + bookId
            );
        }

        return book;
    }

    private StoryEntity findActiveStoryByIdExternal(UUID storyId) {
        return storyRepository.findByIdExternalAndDeletedFalse(storyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Story not found with idExternal: " + storyId
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

    private void validateAuthenticatedUserMatchesReviewer(
            UserEntity authenticatedUser,
            UserEntity reviewer
    ) {
        if (!authenticatedUser.getId().equals(reviewer.getId())) {
            throw new AccessDeniedException(
                    "You cannot create a review on behalf of another user"
            );
        }
    }

    private void validateExactlyOneTarget(UUID bookId, UUID storyId) {
        if ((bookId == null && storyId == null) || (bookId != null && storyId != null)) {
            throw new BadRequestException(
                    "A review must be associated with exactly one target: either a book or a story"
            );
        }
    }

    private void validateDuplicateBookReview(Long reviewerId, Long bookId) {
        if (reviewRepository.existsByReviewerIdAndBookIdAndDeletedFalse(reviewerId, bookId)) {
            throw new DuplicateResourceException(
                    "The user has already reviewed this book"
            );
        }
    }

    private void validateDuplicateStoryReview(Long reviewerId, Long storyId) {
        if (reviewRepository.existsByReviewerIdAndStoryIdAndDeletedFalse(reviewerId, storyId)) {
            throw new DuplicateResourceException(
                    "The user has already reviewed this story"
            );
        }
    }

    private void validateStoryIsNotOwnedByReviewer(StoryEntity story, UserEntity reviewer) {
        if (story.getAuthor() != null
                && story.getAuthor().getId().equals(reviewer.getId())) {
            throw new BadRequestException("Users cannot review their own stories");
        }
    }

    private void validateCanManageReview(ReviewEntity review, UserEntity user) {
        boolean isOwner = review.getReviewer() != null
                && review.getReviewer().getId().equals(user.getId());

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        boolean isModerator = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority()));

        if (!isOwner && !isAdmin && !isModerator) {
            throw new AccessDeniedException(
                    "You do not have permission to manage this review"
            );
        }
    }
}