package com.grupo3.BookVerse.features.reviews.mapper;

import com.grupo3.BookVerse.features.books.repository.BookRepository;
import com.grupo3.BookVerse.features.reviews.domain.ReviewEntity;
import com.grupo3.BookVerse.features.reviews.domain.ReviewTakedownStatus;
import com.grupo3.BookVerse.features.reviews.repository.ReviewRepository;
import com.grupo3.BookVerse.features.reviews.services.impl.ReviewServiceImpl;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewPublicNameTest {
    @Test
    void regularReaderReceivesAnotherReadersPublicNameWithoutProfileAccess() {
        var repository = mock(ReviewRepository.class);
        var users = mock(UserRepository.class);
        var service = new ReviewServiceImpl(repository, Mappers.getMapper(ReviewMapper.class), users,
                mock(BookRepository.class), mock(StoryRepository.class));
        var author = new UserEntity();
        author.setIdExternal(UUID.randomUUID());
        author.setUsername("Public author");
        author.setEmail("private@example.com");
        var review = new ReviewEntity();
        review.setReviewer(author);
        when(repository.findAllByDeletedFalseAndTakedownStatusOrderByCreatedAtDesc(ReviewTakedownStatus.ACTIVE))
                .thenReturn(List.of(review));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "different-reader@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        try {
            var response = service.getAllReviews().getFirst();
            assertEquals("Public author", response.reviewerName());
            assertEquals(author.getIdExternal(), response.reviewerId());
            verifyNoInteractions(users);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
