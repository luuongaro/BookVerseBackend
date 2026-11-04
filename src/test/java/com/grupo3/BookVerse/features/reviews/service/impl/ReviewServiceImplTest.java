package com.grupo3.BookVerse.features.reviews.service.impl;

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
import com.grupo3.BookVerse.features.reviews.services.impl.ReviewServiceImpl;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private UUID reviewId;
    private UUID userId;
    private UUID bookId;
    private UUID storyId;

    private UserEntity user;
    private ReviewEntity review;
    private ReviewResponseDto response;

    @BeforeEach
    void setUp() {

        reviewId = UUID.randomUUID();
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        storyId = UUID.randomUUID();

        user = new UserEntity();
        user.setId(1L);
        user.setIdExternal(userId);

        review = new ReviewEntity();
        review.setId(1L);
        review.setIdExternal(reviewId);
        review.setReviewer(user);
        review.setDeleted(false);

        response = mock(ReviewResponseDto.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null)
        );
    }

    // Verifies that all active reviews are returned correctly
    @Test
    void getAllReviews_shouldReturnList() {

        List<ReviewEntity> reviews = List.of(review);
        List<ReviewResponseDto> dtos = List.of(response);

        when(reviewRepository.findAllByDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
                ReviewTakedownStatus.ACTIVE
        )).thenReturn(reviews);

        when(reviewMapper.toResponseListDto(reviews))
                .thenReturn(dtos);

        List<ReviewResponseDto> result =
                reviewService.getAllReviews();

        assertEquals(dtos, result);

        verify(reviewRepository)
                .findAllByDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
                        ReviewTakedownStatus.ACTIVE
                );
    }

    // Verifies that a review can be found by external id
    @Test
    void getReviewById_shouldReturnDto() {

        when(reviewRepository.findByIdExternalAndDeletedFalseAndTakedownStatus(
                reviewId,
                ReviewTakedownStatus.ACTIVE
        )).thenReturn(Optional.of(review));

        when(reviewMapper.toResponseDto(review))
                .thenReturn(response);

        ReviewResponseDto result =
                reviewService.getReviewById(reviewId);

        assertEquals(response, result);

        verify(reviewRepository)
                .findByIdExternalAndDeletedFalseAndTakedownStatus(
                        reviewId,
                        ReviewTakedownStatus.ACTIVE
                );
    }

    // Verifies exception when review does not exist
    @Test
    void getReviewById_shouldThrow_whenNotFound() {

        when(reviewRepository.findByIdExternalAndDeletedFalseAndTakedownStatus(
                reviewId,
                ReviewTakedownStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getReviewById(reviewId));
    }

    // Verifies reviews by book are returned
    @Test
    void getReviewsByBookId_shouldReturnList() {

        BookEntity book = new BookEntity();
        book.setId(1L);
        book.setDeleted(false);

        List<ReviewEntity> reviews = List.of(review);
        List<ReviewResponseDto> dtos = List.of(response);

        when(bookRepository.findByIdExternal(bookId))
                .thenReturn(Optional.of(book));

        when(reviewRepository
                .findByBookIdAndDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
                        book.getId(),
                        ReviewTakedownStatus.ACTIVE
                ))
                .thenReturn(reviews);

        when(reviewMapper.toResponseListDto(reviews))
                .thenReturn(dtos);

        List<ReviewResponseDto> result =
                reviewService.getReviewsByBookId(bookId);

        assertEquals(dtos, result);
    }

    // Verifies reviews by story are returned
    @Test
    void getReviewsByStoryId_shouldReturnList() {

        StoryEntity story = new StoryEntity();
        story.setId(1L);

        List<ReviewEntity> reviews = List.of(review);
        List<ReviewResponseDto> dtos = List.of(response);

        when(storyRepository.findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.of(story));

        when(reviewRepository
                .findByStoryIdAndDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(
                        story.getId(),
                        ReviewTakedownStatus.ACTIVE
                ))
                .thenReturn(reviews);

        when(reviewMapper.toResponseListDto(reviews))
                .thenReturn(dtos);

        List<ReviewResponseDto> result =
                reviewService.getReviewsByStoryId(storyId);

        assertEquals(dtos, result);
    }

    // Verifies book review creation
    @Test
    void createReview_shouldCreateBookReview() {

        ReviewCreateRequestDto dto =
                new ReviewCreateRequestDto(
                        bookId,
                        null,
                        5,
                        "Very good book review"
                );

        user.setId(1L);

        BookEntity book = new BookEntity();
        book.setId(1L);
        book.setDeleted(false);

        ReviewEntity entity = new ReviewEntity();

        Authentication authentication =
                mock(Authentication.class);

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(user);

        SecurityContextHolder.setContext(securityContext);

        when(bookRepository.findByIdExternal(bookId))
                .thenReturn(Optional.of(book));

        when(reviewMapper.toEntity(dto))
                .thenReturn(entity);

        when(reviewRepository.existsByReviewerIdAndBookIdAndDeletedFalse(
                user.getId(),
                book.getId()
        )).thenReturn(false);

        when(reviewRepository.save(entity))
                .thenReturn(entity);

        when(reviewMapper.toResponseDto(entity))
                .thenReturn(response);

        ReviewResponseDto result =
                reviewService.createReview(dto);

        assertEquals(response, result);

        assertEquals(user, entity.getReviewer());
        assertEquals(book, entity.getBook());
        assertNull(entity.getStory());
        assertEquals(
                ReviewTakedownStatus.ACTIVE,
                entity.getTakedownStatus()
        );

        verify(reviewRepository)
                .save(entity);
    }
    // Verifies story review creation
    @Test
    void createReview_shouldCreateStoryReview() {

        ReviewCreateRequestDto dto =
                new ReviewCreateRequestDto(
                        null,
                        storyId,
                        5,
                        "Very good story review"
                );

        StoryEntity story = new StoryEntity();
        story.setId(1L);

        ReviewEntity entity = new ReviewEntity();

        when(storyRepository.findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.of(story));

        when(reviewMapper.toEntity(dto))
                .thenReturn(entity);

        when(reviewRepository.save(entity))
                .thenReturn(entity);

        when(reviewMapper.toResponseDto(entity))
                .thenReturn(response);

        ReviewResponseDto result =
                reviewService.createReview(dto);

        assertEquals(response, result);
    }

    // Verifies exception when no target is provided
    @Test
    void createReview_shouldThrow_whenNoTarget() {

        ReviewCreateRequestDto dto =
                new ReviewCreateRequestDto(
                        null,
                        null,
                        5,
                        "Good review content"
                );

        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(dto));
    }

    // Verifies exception when both targets are provided
    @Test
    void createReview_shouldThrow_whenBothTargets() {

        ReviewCreateRequestDto dto =
                new ReviewCreateRequestDto(
                        bookId,
                        storyId,
                        5,
                        "Good review content"
                );

        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(dto));
    }

    // Verifies duplicate book review exception
    @Test
    void createReview_shouldThrow_whenDuplicateBookReview() {

        ReviewCreateRequestDto dto =
                new ReviewCreateRequestDto(
                        bookId,
                        null,
                        5,
                        "Good review content"
                );

        BookEntity book = new BookEntity();
        book.setId(1L);
        book.setDeleted(false);

        user.setId(1L);

        ReviewEntity review = new ReviewEntity();

        Authentication authentication =
                mock(Authentication.class);

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(user);

        SecurityContextHolder.setContext(securityContext);

        when(bookRepository.findByIdExternal(bookId))
                .thenReturn(Optional.of(book));

        when(reviewMapper.toEntity(dto))
                .thenReturn(review);

        when(reviewRepository.existsByReviewerIdAndBookIdAndDeletedFalse(
                user.getId(),
                book.getId()
        )).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> reviewService.createReview(dto)
        );

        verify(reviewRepository, never())
                .save(any());
    }

    // Verifies review update
    @Test
    void updateReview_shouldUpdateReview() {

        ReviewUpdateRequestDto dto =
                new ReviewUpdateRequestDto(
                        4,
                        "Updated review content"
                );

        when(reviewRepository.findByIdExternalAndDeletedFalseAndTakedownStatus(
                reviewId,
                ReviewTakedownStatus.ACTIVE
        )).thenReturn(Optional.of(review));

        when(reviewRepository.save(review))
                .thenReturn(review);

        when(reviewMapper.toResponseDto(review))
                .thenReturn(response);

        ReviewResponseDto result =
                reviewService.updateReview(reviewId, dto);

        assertEquals(response, result);
        assertEquals(4, review.getRating());
        assertEquals("Updated review content",
                review.getContent());
    }

    // Verifies logical deletion of review
    @Test
    void deleteReview_shouldSoftDelete() {

        when(reviewRepository.findByIdExternalAndDeletedFalseAndTakedownStatus(
                reviewId,
                ReviewTakedownStatus.ACTIVE
        )).thenReturn(Optional.of(review));

        reviewService.deleteReview(reviewId);

        assertTrue(review.isDeleted());

        verify(reviewRepository)
                .save(review);
    }

    // Verifies access denied when user is not owner
    @Test
    void updateReview_shouldThrow_whenUnauthorized() {

        UUID reviewId = UUID.randomUUID();

        ReviewUpdateRequestDto dto =
                new ReviewUpdateRequestDto(
                        5,
                        "Updated review content"
                );

        UserEntity owner = mock(UserEntity.class);
        UserEntity anotherUser = mock(UserEntity.class);

        ReviewEntity review = new ReviewEntity();
        review.setReviewer(owner);

        Authentication authentication =
                mock(Authentication.class);

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(owner.getId())
                .thenReturn(1L);

        when(anotherUser.getId())
                .thenReturn(2L);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        when(authentication.getPrincipal())
                .thenReturn(anotherUser);

        SecurityContextHolder.setContext(securityContext);

        when(reviewRepository.findByIdExternalAndDeletedFalseAndTakedownStatus(
                reviewId,
                ReviewTakedownStatus.ACTIVE
        )).thenReturn(Optional.of(review));

        assertThrows(
                AccessDeniedException.class,
                () -> reviewService.updateReview(reviewId, dto)
        );

        verify(reviewRepository, never())
                .save(any());
    }
}
