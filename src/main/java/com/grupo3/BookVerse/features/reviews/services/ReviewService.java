package com.grupo3.BookVerse.features.reviews.services;

import com.grupo3.BookVerse.features.reviews.dto.ReviewCreateRequestDto;
import com.grupo3.BookVerse.features.reviews.dto.ReviewRatingSummaryDto;
import com.grupo3.BookVerse.features.reviews.dto.ReviewResponseDto;
import com.grupo3.BookVerse.features.reviews.dto.ReviewUpdateRequestDto;

import java.util.List;
import java.util.UUID;

public interface ReviewService {

    List<ReviewResponseDto> getAllReviews();

    ReviewResponseDto getReviewById(UUID reviewId);

    List<ReviewResponseDto> getReviewsByBookId(UUID bookId);

    List<ReviewResponseDto> getReviewsByStoryId(UUID storyId);

    ReviewResponseDto createReview(ReviewCreateRequestDto reviewCreateRequestDto);

    ReviewResponseDto updateReview(UUID reviewId, ReviewUpdateRequestDto reviewUpdateRequestDto);

    void deleteReview(UUID reviewId);

    ReviewRatingSummaryDto getBookRatingSummary(UUID bookId);

    ReviewRatingSummaryDto getStoryRatingSummary(UUID storyId);
}
