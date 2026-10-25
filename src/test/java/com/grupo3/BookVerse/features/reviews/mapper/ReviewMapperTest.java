package com.grupo3.BookVerse.features.reviews.mapper;

import com.grupo3.BookVerse.features.reviews.domain.ReviewEntity;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ReviewMapperTest {
    private final ReviewMapper mapper = Mappers.getMapper(ReviewMapper.class);

    @Test
    void includesPublicUsernameWithoutUsingLoginEmail() {
        UserEntity reader = new UserEntity();
        reader.setIdExternal(UUID.randomUUID());
        reader.setUsername("Public reader");
        reader.setEmail("private@example.com");
        ReviewEntity review = new ReviewEntity();
        review.setReviewer(reader);
        review.setRating(4);
        var result = mapper.toResponseDto(review);
        assertEquals("Public reader", result.reviewerName());
        assertEquals(reader.getIdExternal(), result.reviewerId());
        assertNotEquals(reader.getUsername(), result.reviewerName());
        assertEquals("Public reader", mapper.toResponseListDto(List.of(review)).getFirst().reviewerName());
    }

    @Test
    void missingReviewerDoesNotBreakMapping() {
        var result = mapper.toResponseDto(new ReviewEntity());
        assertNull(result.reviewerName());
    }
}
