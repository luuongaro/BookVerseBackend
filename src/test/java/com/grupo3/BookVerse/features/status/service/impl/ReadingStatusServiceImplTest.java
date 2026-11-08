package com.grupo3.BookVerse.features.status.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.books.repository.BookRepository;
import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import com.grupo3.BookVerse.features.chapters.repository.ChapterRepository;
import com.grupo3.BookVerse.features.roles.domain.RoleEntity;
import com.grupo3.BookVerse.features.status.domain.ProgressType;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEnum;
import com.grupo3.BookVerse.features.status.dto.ReadingStatusRequestDto;
import com.grupo3.BookVerse.features.status.dto.ReadingStatusResponseDto;
import com.grupo3.BookVerse.features.status.mappers.ReadingStatusMapper;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;
import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class ReadingStatusServiceImplTest {

    @Mock
    private ReadingStatusRepository readingStatusRepository;

    @Mock
    private ReadingStatusMapper readingStatusMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @InjectMocks
    private ReadingStatusServiceImpl readingStatusService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesShelfPositionsForAuthenticatedUsersReadings() {
        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);
        authenticateAs(user);
        ReadingStatusEntity first = ReadingStatusEntity.builder().idExternal(UUID.randomUUID()).user(user)
                .book(BookEntity.builder().build()).status(ReadingStatusEnum.IN_PROGRESS).build();
        ReadingStatusEntity second = ReadingStatusEntity.builder().idExternal(UUID.randomUUID()).user(user)
                .book(BookEntity.builder().build()).status(ReadingStatusEnum.IN_PROGRESS).build();
        when(readingStatusRepository.findByUserIdExternal(user.getIdExternal())).thenReturn(List.of(first, second));
        readingStatusService.updateShelfOrder(List.of(second.getIdExternal(), first.getIdExternal()));
        assertEquals(0, second.getShelfPosition());
        assertEquals(1, first.getShelfPosition());
        verify(readingStatusRepository).saveAll(any());
    }

    @Test
    void rejectsOtherUsersAndMixedShelvesWithoutSaving() {
        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);
        authenticateAs(user);
        ReadingStatusEntity first = ReadingStatusEntity.builder().idExternal(UUID.randomUUID()).user(user)
                .book(BookEntity.builder().build()).status(ReadingStatusEnum.IN_PROGRESS).build();
        ReadingStatusEntity second = ReadingStatusEntity.builder().idExternal(UUID.randomUUID()).user(user)
                .book(BookEntity.builder().build()).status(ReadingStatusEnum.FINISHED).build();
        when(readingStatusRepository.findByUserIdExternal(user.getIdExternal())).thenReturn(List.of(first, second));
        assertThrows(AccessDeniedException.class, () -> readingStatusService.updateShelfOrder(List.of(first.getIdExternal(), UUID.randomUUID())));
        assertThrows(BadRequestException.class, () -> readingStatusService.updateShelfOrder(List.of(first.getIdExternal(), second.getIdExternal())));
        assertThrows(BadRequestException.class, () -> readingStatusService.updateShelfOrder(List.of(first.getIdExternal(), first.getIdExternal())));
        verify(readingStatusRepository, never()).saveAll(any());
    }

    @Test
    void shouldCreateReadingStatusForBook() {
        UUID bookId = UUID.randomUUID();

        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);
        BookEntity book = BookEntity.builder()
                .idExternal(bookId)
                .title("Clean Code")
                .build();

        ReadingStatusRequestDto request = buildBookRequest(bookId);

        ReadingStatusEntity mappedEntity = ReadingStatusEntity.builder()
                .status(ReadingStatusEnum.IN_PROGRESS)
                .build();

        ReadingStatusEntity savedEntity = ReadingStatusEntity.builder()
                .idExternal(UUID.randomUUID())
                .user(user)
                .book(book)
                .status(ReadingStatusEnum.IN_PROGRESS)
                .build();

        ReadingStatusResponseDto response = ReadingStatusResponseDto.builder()
                .idExternal(savedEntity.getIdExternal())
                .userId(user.getIdExternal())
                .bookId(bookId)
                .status(ReadingStatusEnum.IN_PROGRESS)
                .progressType(ProgressType.PERCENTAGE)
                .progressValue(25)
                .build();

        authenticateAs(user);

        when(bookRepository.findByIdExternal(bookId))
                .thenReturn(Optional.of(book));
        when(readingStatusMapper.toEntity(request))
                .thenReturn(mappedEntity);
        when(readingStatusRepository.save(any(ReadingStatusEntity.class)))
                .thenReturn(savedEntity);
        when(readingStatusMapper.toResponseDto(savedEntity))
                .thenReturn(response);

        ReadingStatusResponseDto result =
                readingStatusService.createReadingStatus(request);

        assertNotNull(result);
        assertEquals(bookId, result.getBookId());
        assertEquals(ReadingStatusEnum.IN_PROGRESS, result.getStatus());

        ArgumentCaptor<ReadingStatusEntity> captor =
                ArgumentCaptor.forClass(ReadingStatusEntity.class);

        verify(readingStatusRepository)
                .save(captor.capture());

        ReadingStatusEntity entityToSave =
                captor.getValue();

        assertEquals(user, entityToSave.getUser());
        assertEquals(book, entityToSave.getBook());
        assertNull(entityToSave.getStory());
    }

    @Test
    void shouldThrowExceptionWhenRequestHasBookAndStory() {
        ReadingStatusRequestDto request =
                new ReadingStatusRequestDto(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ReadingStatusEnum.IN_PROGRESS,
                        null,
                        null,
                        ProgressType.PERCENTAGE,
                        25
                );

        assertThrows(
                BadRequestException.class,
                () -> readingStatusService.createReadingStatus(request)
        );

        verify(readingStatusRepository, never())
                .save(any(ReadingStatusEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenRequestHasNoBookOrStory() {
        ReadingStatusRequestDto request =
                new ReadingStatusRequestDto(
                        null,
                        null,
                        ReadingStatusEnum.IN_PROGRESS,
                        null,
                        null,
                        ProgressType.PERCENTAGE,
                        25
                );

        assertThrows(
                BadRequestException.class,
                () -> readingStatusService.createReadingStatus(request)
        );

        verify(readingStatusRepository, never())
                .save(any(ReadingStatusEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenProgressTypeAndValueAreNotBothProvided() {
        ReadingStatusRequestDto request =
                new ReadingStatusRequestDto(
                        UUID.randomUUID(),
                        null,
                        ReadingStatusEnum.IN_PROGRESS,
                        null,
                        null,
                        ProgressType.PERCENTAGE,
                        null
                );

        assertThrows(
                BadRequestException.class,
                () -> readingStatusService.createReadingStatus(request)
        );

        verify(readingStatusRepository, never())
                .save(any(ReadingStatusEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenPercentageProgressExceedsOneHundred() {
        ReadingStatusRequestDto request =
                new ReadingStatusRequestDto(
                        UUID.randomUUID(),
                        null,
                        ReadingStatusEnum.IN_PROGRESS,
                        null,
                        null,
                        ProgressType.PERCENTAGE,
                        101
                );

        assertThrows(
                BadRequestException.class,
                () -> readingStatusService.createReadingStatus(request)
        );

        verify(readingStatusRepository, never())
                .save(any(ReadingStatusEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenStoryChapterDoesNotExist() {
        UUID storyId = UUID.randomUUID();

        ReadingStatusRequestDto request =
                new ReadingStatusRequestDto(
                        null,
                        storyId,
                        ReadingStatusEnum.IN_PROGRESS,
                        null,
                        null,
                        ProgressType.CHAPTER,
                        3
                );

        when(chapterRepository.findByStoryIdExternalAndChapterNumber(storyId, 3))
                .thenReturn(Optional.empty());

        assertThrows(
                BadRequestException.class,
                () -> readingStatusService.createReadingStatus(request)
        );

        verify(readingStatusRepository, never())
                .save(any(ReadingStatusEntity.class));
    }

    @Test
    void shouldDenyFreeUserReadingPremiumStory() {
        UUID storyId = UUID.randomUUID();

        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);
        StoryEntity story = buildStory(storyId, StoryAccessType.PREMIUM, buildUser(2L, "author@mail.com", "ROLE_USER", SubscriptionType.FREE));

        ReadingStatusRequestDto request =
                new ReadingStatusRequestDto(
                        null,
                        storyId,
                        ReadingStatusEnum.IN_PROGRESS,
                        null,
                        null,
                        null,
                        null
                );

        authenticateAs(user);

        when(storyRepository.findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.of(story));

        assertThrows(
                AccessDeniedException.class,
                () -> readingStatusService.createReadingStatus(request)
        );

        verify(readingStatusRepository, never())
                .save(any(ReadingStatusEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenFreeUserExceedsActiveStoryLimit() {
        UUID storyId = UUID.randomUUID();

        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);
        user.getSubscription().setMaxActiveStoriesReading(1);

        StoryEntity story = buildStory(storyId, StoryAccessType.FREE, buildUser(2L, "author@mail.com", "ROLE_USER", SubscriptionType.FREE));

        ReadingStatusRequestDto request =
                new ReadingStatusRequestDto(
                        null,
                        storyId,
                        ReadingStatusEnum.IN_PROGRESS,
                        null,
                        null,
                        null,
                        null
                );

        authenticateAs(user);

        when(storyRepository.findByIdExternalAndDeletedFalse(storyId))
                .thenReturn(Optional.of(story));
        when(readingStatusRepository.countDistinctActiveStoriesByUserExcludingOwnStories(
                eq(user.getIdExternal()),
                anyList()
        )).thenReturn(1L);
        when(readingStatusRepository.existsActiveReadingByUserAndStory(
                eq(user.getIdExternal()),
                eq(storyId),
                anyList()
        )).thenReturn(false);

        assertThrows(
                BadRequestException.class,
                () -> readingStatusService.createReadingStatus(request)
        );

        verify(readingStatusRepository, never())
                .save(any(ReadingStatusEntity.class));
    }

    @Test
    void shouldReturnReadingStatusByIdExternalWhenOwner() {
        UUID idExternal = UUID.randomUUID();

        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);

        ReadingStatusEntity entity = ReadingStatusEntity.builder()
                .idExternal(idExternal)
                .user(user)
                .status(ReadingStatusEnum.IN_PROGRESS)
                .build();

        ReadingStatusResponseDto response = ReadingStatusResponseDto.builder()
                .idExternal(idExternal)
                .userId(user.getIdExternal())
                .status(ReadingStatusEnum.IN_PROGRESS)
                .build();

        authenticateAs(user);

        when(readingStatusRepository.findByIdExternal(idExternal))
                .thenReturn(Optional.of(entity));
        when(readingStatusMapper.toResponseDto(entity))
                .thenReturn(response);

        ReadingStatusResponseDto result =
                readingStatusService.getReadingStatusByIdExternal(idExternal);

        assertNotNull(result);
        assertEquals(idExternal, result.getIdExternal());

        verify(readingStatusMapper)
                .toResponseDto(entity);
    }

    @Test
    void shouldThrowExceptionWhenReadingStatusDoesNotExist() {
        UUID idExternal = UUID.randomUUID();

        when(readingStatusRepository.findByIdExternal(idExternal))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> readingStatusService.getReadingStatusByIdExternal(idExternal)
        );

        verify(readingStatusMapper, never())
                .toResponseDto(any(ReadingStatusEntity.class));
    }

    @Test
    void shouldReturnAllReadingStatusesWhenAuthenticatedUserIsAdmin() {
        UserEntity admin = buildUser(1L, "admin@mail.com", "ROLE_ADMIN", SubscriptionType.PREMIUM);

        ReadingStatusEntity entity = ReadingStatusEntity.builder()
                .idExternal(UUID.randomUUID())
                .user(admin)
                .status(ReadingStatusEnum.FINISHED)
                .build();

        ReadingStatusResponseDto response = ReadingStatusResponseDto.builder()
                .idExternal(entity.getIdExternal())
                .userId(admin.getIdExternal())
                .status(ReadingStatusEnum.FINISHED)
                .build();

        authenticateAs(admin);

        when(readingStatusRepository.findAll())
                .thenReturn(List.of(entity));
        when(readingStatusMapper.toResponseDtoList(List.of(entity)))
                .thenReturn(List.of(response));

        List<ReadingStatusResponseDto> result =
                readingStatusService.getAllReadingStatuses();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ReadingStatusEnum.FINISHED, result.get(0).getStatus());

        verify(readingStatusRepository)
                .findAll();
    }

    @Test
    void shouldDenyGetAllReadingStatusesWhenAuthenticatedUserIsRegularUser() {
        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);

        authenticateAs(user);

        assertThrows(
                AccessDeniedException.class,
                () -> readingStatusService.getAllReadingStatuses()
        );

        verify(readingStatusRepository, never())
                .findAll();
    }

    @Test
    void shouldReturnReadingStatusesByUserWhenAuthenticatedUserIsSelf() {
        UUID userId = UUID.randomUUID();

        UserEntity user = buildUser(1L, userId, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);

        ReadingStatusEntity entity = ReadingStatusEntity.builder()
                .idExternal(UUID.randomUUID())
                .user(user)
                .status(ReadingStatusEnum.WANT_TO_READ)
                .build();

        ReadingStatusResponseDto response = ReadingStatusResponseDto.builder()
                .idExternal(entity.getIdExternal())
                .userId(userId)
                .status(ReadingStatusEnum.WANT_TO_READ)
                .build();

        authenticateAs(user);

        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(user));
        when(readingStatusRepository.findByUserIdExternal(userId))
                .thenReturn(List.of(entity));
        when(readingStatusMapper.toResponseDtoList(List.of(entity)))
                .thenReturn(List.of(response));

        List<ReadingStatusResponseDto> result =
                readingStatusService.getReadingStatusesByUser(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
    }

    @Test
    void shouldUpdateReadingStatusWhenAuthenticatedUserIsOwner() {
        UUID idExternal = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);
        BookEntity book = BookEntity.builder()
                .idExternal(bookId)
                .title("Updated Book")
                .build();

        ReadingStatusEntity entity = ReadingStatusEntity.builder()
                .idExternal(idExternal)
                .user(user)
                .status(ReadingStatusEnum.WANT_TO_READ)
                .build();

        ReadingStatusRequestDto request =
                new ReadingStatusRequestDto(
                        bookId,
                        null,
                        ReadingStatusEnum.FINISHED,
                        null,
                        null,
                        ProgressType.PERCENTAGE,
                        100
                );

        ReadingStatusResponseDto response = ReadingStatusResponseDto.builder()
                .idExternal(idExternal)
                .userId(user.getIdExternal())
                .bookId(bookId)
                .status(ReadingStatusEnum.FINISHED)
                .progressType(ProgressType.PERCENTAGE)
                .progressValue(100)
                .build();

        authenticateAs(user);

        when(readingStatusRepository.findByIdExternal(idExternal))
                .thenReturn(Optional.of(entity));
        when(bookRepository.findByIdExternal(bookId))
                .thenReturn(Optional.of(book));
        when(readingStatusRepository.save(entity))
                .thenReturn(entity);
        when(readingStatusMapper.toResponseDto(entity))
                .thenReturn(response);

        ReadingStatusResponseDto result =
                readingStatusService.updateReadingStatus(idExternal, request);

        assertNotNull(result);
        assertEquals(ReadingStatusEnum.FINISHED, result.getStatus());
        assertEquals(ProgressType.PERCENTAGE, entity.getProgressType());
        assertEquals(100, entity.getProgressValue());
        assertEquals(book, entity.getBook());
        assertNull(entity.getStory());

        verify(readingStatusRepository)
                .save(entity);
    }

    @Test
    void shouldDeleteReadingStatusWhenAuthenticatedUserIsOwner() {
        UUID idExternal = UUID.randomUUID();

        UserEntity user = buildUser(1L, "reader@mail.com", "ROLE_USER", SubscriptionType.FREE);

        ReadingStatusEntity entity = ReadingStatusEntity.builder()
                .idExternal(idExternal)
                .user(user)
                .status(ReadingStatusEnum.WANT_TO_READ)
                .build();

        authenticateAs(user);

        when(readingStatusRepository.findByIdExternal(idExternal))
                .thenReturn(Optional.of(entity));

        readingStatusService.deleteReadingStatus(idExternal);

        verify(readingStatusRepository)
                .delete(entity);
    }

    private ReadingStatusRequestDto buildBookRequest(UUID bookId) {
        return new ReadingStatusRequestDto(
                bookId,
                null,
                ReadingStatusEnum.IN_PROGRESS,
                null,
                null,
                ProgressType.PERCENTAGE,
                25
        );
    }

    private StoryEntity buildStory(UUID idExternal, StoryAccessType accessType, UserEntity author) {
        return StoryEntity.builder()
                .id(1L)
                .idExternal(idExternal)
                .title("Story")
                .description("Description")
                .accessType(accessType)
                .author(author)
                .build();
    }

    private UserEntity buildUser(Long id, String email, String roleName, SubscriptionType subscriptionType) {
        return buildUser(id, UUID.randomUUID(), email, roleName, subscriptionType);
    }

    private UserEntity buildUser(Long id, UUID idExternal, String email, String roleName, SubscriptionType subscriptionType) {
        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .type(subscriptionType)
                .maxActiveStoriesReading(subscriptionType == SubscriptionType.FREE ? 1 : Integer.MAX_VALUE)
                .build();

        UserEntity user = UserEntity.builder()
                .id(id)
                .idExternal(idExternal)
                .username(email.split("@")[0])
                .email(email)
                .subscription(subscription)
                .build();

        user.getRoles().add(
                RoleEntity.builder()
                        .name(roleName)
                        .build()
        );

        return user;
    }

    private void authenticateAs(UserEntity user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }
}
