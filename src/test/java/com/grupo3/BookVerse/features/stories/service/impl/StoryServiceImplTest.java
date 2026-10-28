package com.grupo3.BookVerse.features.stories.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.chapters.mappers.ChapterMapper;
import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.dto.StoryDetailResponseDto;
import com.grupo3.BookVerse.features.stories.dto.StoryRequestDto;
import com.grupo3.BookVerse.features.stories.dto.StoryResponseDto;
import com.grupo3.BookVerse.features.stories.mappers.StoryMapper;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryServiceImplTest {

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StoryMapper storyMapper;

    @Mock
    private ChapterMapper chapterMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StoryServiceImpl storyService;

    private UserEntity user;
    private StoryEntity story;
    private StoryRequestDto requestDto;
    private StoryResponseDto responseDto;
    private UUID userId;
    private UUID storyId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        userId = UUID.randomUUID();
        storyId = UUID.randomUUID();

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setType(SubscriptionType.PREMIUM);
        subscription.setMaxStoriesPublished(10);

        user = new UserEntity();
        user.setId(1L);
        user.setIdExternal(userId);
        user.setSubscription(subscription);

        story = StoryEntity.builder()
                .id(1L)
                .idExternal(storyId)
                .title("Historia")
                .description("Descripción")
                .accessType(StoryAccessType.FREE)
                .deleted(false)
                .hidden(false)
                .author(user)
                .build();

        requestDto = StoryRequestDto.builder()
                .authorId(userId)
                .title("Historia")
                .description("Descripción")
                .accessType(StoryAccessType.FREE)
                .build();

        responseDto = StoryResponseDto.builder()
                .idExternal(storyId)
                .title("Historia")
                .description("Descripción")
                .build();

        setAuthenticatedUser(user);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(UserEntity user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of()
        );

        SecurityContextHolder.getContext()
                .setAuthentication(auth);
    }

    @Test
    void createStory_ShouldCreateSuccessfully() {

        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(user));

        when(storyRepository.countByAuthorIdAndDeletedFalse(1L))
                .thenReturn(0L);

        when(storyMapper.toEntity(requestDto))
                .thenReturn(story);

        when(storyRepository.save(any(StoryEntity.class)))
                .thenReturn(story);

        when(storyMapper.toResponseDto(story))
                .thenReturn(responseDto);

        StoryResponseDto result =
                storyService.createStory(requestDto);

        assertNotNull(result);
        assertEquals("Historia", result.getTitle());

        verify(storyRepository).save(any(StoryEntity.class));
    }

    @Test
    void createStory_ShouldThrowWhenUserNotFound() {

        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.createStory(requestDto)
        );
    }

    @Test
    void createStory_ShouldThrowWhenDifferentAuthor() {

        UserEntity anotherUser = new UserEntity();
        anotherUser.setId(99L);

        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(anotherUser));

        assertThrows(
                AccessDeniedException.class,
                () -> storyService.createStory(requestDto)
        );
    }

    @Test
    void createStory_ShouldThrowWhenMaxStoriesReached() {

        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(user));

        when(storyRepository.countByAuthorIdAndDeletedFalse(1L))
                .thenReturn(10L);

        assertThrows(
                BadRequestException.class,
                () -> storyService.createStory(requestDto)
        );
    }

    @Test
    void getAllStories_ShouldReturnPage() {

        Page<StoryEntity> storyPage =
                new PageImpl<>(List.of(story));

        when(storyRepository
                .findAllByDeletedFalseOrderByCreatedAtDesc(any()))
                .thenReturn(storyPage);

        when(storyMapper.toResponseDto(story))
                .thenReturn(responseDto);

        Page<StoryResponseDto> result =
                storyService.getAllStories(PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
    }

    @Test
    void getStoryByIdExternal_ShouldReturnStory() {

        when(storyRepository
                .findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.of(story));

        StoryDetailResponseDto result =
                storyService.getStoryByIdExternal(storyId);

        assertNotNull(result);
        assertEquals("Historia", result.getTitle());
    }

    @Test
    void getStoryByIdExternal_ShouldThrowWhenNotFound() {

        when(storyRepository
                .findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> storyService.getStoryByIdExternal(storyId)
        );
    }

    @Test
    void updateStory_ShouldUpdateSuccessfully() {

        when(storyRepository
                .findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.of(story));

        when(storyRepository.save(any()))
                .thenReturn(story);

        when(storyMapper.toResponseDto(story))
                .thenReturn(responseDto);

        StoryResponseDto result =
                storyService.updateStory(storyId, requestDto);

        assertNotNull(result);
        verify(storyRepository).save(story);
    }

    @Test
    void updateStory_ShouldThrowWhenNotOwner() {

        UserEntity anotherUser = new UserEntity();
        anotherUser.setId(999L);

        setAuthenticatedUser(anotherUser);

        when(storyRepository
                .findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.of(story));

        assertThrows(
                AccessDeniedException.class,
                () -> storyService.updateStory(storyId, requestDto)
        );
    }

    @Test
    void deleteStory_ShouldDeleteSuccessfully() {

        when(storyRepository
                .findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.of(story));

        when(storyRepository.save(any()))
                .thenReturn(story);

        storyService.deleteStory(storyId);

        assertTrue(story.isDeleted());

        verify(storyRepository).save(story);
    }

    @Test
    void getStoriesByAuthorId_ShouldReturnStories() {

        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(user));

        Page<StoryEntity> page =
                new PageImpl<>(List.of(story));

        when(storyRepository
                .findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(
                        eq(1L),
                        any(Pageable.class)
                ))
                .thenReturn(page);

        when(storyMapper.toResponseDto(story))
                .thenReturn(responseDto);

        Page<StoryResponseDto> result =
                storyService.getStoriesByAuthorId(
                        userId,
                        PageRequest.of(0, 10)
                );

        assertEquals(1, result.getContent().size());
    }

    @Test
    void createStory_ShouldThrowWhenPremiumStoryAndUserFree() {

        SubscriptionEntity freeSubscription =
                new SubscriptionEntity();

        freeSubscription.setType(SubscriptionType.FREE);
        freeSubscription.setMaxStoriesPublished(10);

        user.setSubscription(freeSubscription);

        requestDto.setAccessType(
                StoryAccessType.PREMIUM
        );

        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(user));

        when(storyRepository.countByAuthorIdAndDeletedFalse(1L))
                .thenReturn(0L);

        assertThrows(
                AccessDeniedException.class,
                () -> storyService.createStory(requestDto)
        );
    }
}