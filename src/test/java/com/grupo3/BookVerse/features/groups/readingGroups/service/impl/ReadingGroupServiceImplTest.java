package com.grupo3.BookVerse.features.groups.readingGroups.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.books.repository.BookRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.UpdateReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.mappers.ReadingGroupMapper;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingGroupServiceImplTest {

    @Mock
    private ReadingGroupRepository repository;

    @Mock
    private ReadingGroupMapper mapper;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private ReadingGroupServiceImpl service;

    private UUID groupId;
    private UUID bookId;
    private UUID storyId;
    private UserEntity authenticatedUser;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        bookId = UUID.randomUUID();
        storyId = UUID.randomUUID();

        // Mock authenticated user in SecurityContext
        authenticatedUser = new UserEntity();
        authenticatedUser.setIdExternal(UUID.randomUUID());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Verify that a reading group is created successfully with a book
    @Test
    void createGroup_shouldReturnResponseDto_whenBookProvided() {

        ReadingGroupRequestDto request =
                new ReadingGroupRequestDto(
                        bookId,
                        null,
                        "Fantasy Readers",
                        true
                );

        BookEntity book = new BookEntity();
        ReadingGroupEntity entity = new ReadingGroupEntity();
        ReadingGroupEntity saved = new ReadingGroupEntity();

        ReadingGroupResponseDto response =
                new ReadingGroupResponseDto(
                        groupId,
                        bookId,
                        null,
                        null,
                        "Fantasy Readers",
                        true,
                        null
                );

        when(bookRepository.findByIdExternal(bookId))
                .thenReturn(Optional.of(book));

        when(mapper.toEntity(request))
                .thenReturn(entity);

        when(repository.save(entity))
                .thenReturn(saved);

        when(mapper.toResponseDto(saved))
                .thenReturn(response);

        ReadingGroupResponseDto result =
                service.createGroup(request);

        assertNotNull(result);
        assertEquals(response, result);

        verify(repository).save(entity);
    }

    // Verify that a reading group is created successfully with a story
    @Test
    void createGroup_shouldReturnResponseDto_whenStoryProvided() {

        ReadingGroupRequestDto request =
                new ReadingGroupRequestDto(
                        null,
                        storyId,
                        "Manga Readers",
                        true
                );

        StoryEntity story = new StoryEntity();
        ReadingGroupEntity entity = new ReadingGroupEntity();
        ReadingGroupEntity saved = new ReadingGroupEntity();

        ReadingGroupResponseDto response =
                mock(ReadingGroupResponseDto.class);

        when(storyRepository.findByIdExternal(storyId))
                .thenReturn(Optional.of(story));

        when(mapper.toEntity(request))
                .thenReturn(entity);

        when(repository.save(entity))
                .thenReturn(saved);

        when(mapper.toResponseDto(saved))
                .thenReturn(response);

        ReadingGroupResponseDto result =
                service.createGroup(request);

        assertEquals(response, result);
    }

    // Verify that an exception is thrown when both bookId and storyId are null
    @Test
    void createGroup_shouldThrowException_whenBookAndStoryAreNull() {

        ReadingGroupRequestDto request =
                new ReadingGroupRequestDto(
                        null,
                        null,
                        "Group",
                        true
                );

        assertThrows(
                BadRequestException.class,
                () -> service.createGroup(request)
        );
    }

    // Verify that an exception is thrown when both bookId and storyId are provided
    @Test
    void createGroup_shouldThrowException_whenBookAndStoryExist() {

        ReadingGroupRequestDto request =
                new ReadingGroupRequestDto(
                        bookId,
                        storyId,
                        "Group",
                        true
                );

        assertThrows(
                BadRequestException.class,
                () -> service.createGroup(request)
        );
    }

    // Verify that all reading groups are returned correctly
    @Test
    void getAllGroups_shouldReturnList() {

        ReadingGroupEntity activeEntity = new ReadingGroupEntity();
        activeEntity.setIsActive(true);

        List<ReadingGroupEntity> entities = List.of(activeEntity);

        List<ReadingGroupResponseDto> responses =
                List.of(mock(ReadingGroupResponseDto.class));

        when(repository.findAll())
                .thenReturn(entities);

        when(mapper.toResponseDtoList(List.of(activeEntity)))
                .thenReturn(responses);

        List<ReadingGroupResponseDto> result =
                service.getAllGroups();

        assertEquals(responses, result);

        verify(repository).findAll();
    }

    // Verify that a reading group is returned by external ID
    @Test
    void getGroupByIdExternal_shouldReturnDto() {

        ReadingGroupEntity entity = new ReadingGroupEntity();
        entity.setIsActive(true);

        ReadingGroupResponseDto response =
                mock(ReadingGroupResponseDto.class);

        when(repository.findByIdExternal(groupId))
                .thenReturn(Optional.of(entity));

        when(mapper.toResponseDto(entity))
                .thenReturn(response);

        ReadingGroupResponseDto result =
                service.getGroupByIdExternal(groupId);

        assertEquals(response, result);
    }

    // Verify that an exception is thrown when the group is not found
    @Test
    void getGroupByIdExternal_shouldThrowException_whenNotFound() {

        when(repository.findByIdExternal(groupId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getGroupByIdExternal(groupId)
        );
    }

    // Verify that reading groups are returned by book ID
    @Test
    void getGroupsByBookIdExternal_shouldReturnList() {

        BookEntity book = new BookEntity();

        ReadingGroupEntity activeEntity = new ReadingGroupEntity();
        activeEntity.setIsActive(true);

        List<ReadingGroupEntity> entities = List.of(activeEntity);

        List<ReadingGroupResponseDto> responses =
                List.of(mock(ReadingGroupResponseDto.class));

        when(bookRepository.findByIdExternal(bookId))
                .thenReturn(Optional.of(book));

        when(repository.findByBook_IdExternal(bookId))
                .thenReturn(entities);

        when(mapper.toResponseDtoList(List.of(activeEntity)))
                .thenReturn(responses);

        List<ReadingGroupResponseDto> result =
                service.getGroupsByBookIdExternal(bookId);

        assertEquals(responses, result);
    }

    // Verify that reading groups are returned by user ID
    @Test
    void getGroupsByUserIdExternal_shouldReturnList() {

        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();

        ReadingGroupEntity activeEntity = new ReadingGroupEntity();
        activeEntity.setIsActive(true);

        List<ReadingGroupEntity> entities = List.of(activeEntity);

        List<ReadingGroupResponseDto> responses =
                List.of(mock(ReadingGroupResponseDto.class));

        when(userRepository.findByIdExternal(userId))
                .thenReturn(Optional.of(user));

        when(repository.findByCreatedBy_IdExternal(userId))
                .thenReturn(entities);

        when(mapper.toResponseDtoList(List.of(activeEntity)))
                .thenReturn(responses);

        List<ReadingGroupResponseDto> result =
                service.getGroupsByUserIdExternal(userId);

        assertEquals(responses, result);
    }

    // Verify that a reading group is updated successfully
    @Test
    void updateGroup_shouldReturnUpdatedDto() {

        UpdateReadingGroupRequestDto request =
                new UpdateReadingGroupRequestDto(
                        "Updated Group"
                );

        BookEntity book = new BookEntity();
        book.setIdExternal(bookId);

        ReadingGroupEntity entity = new ReadingGroupEntity();
        entity.setIsActive(true);
        entity.setBook(book);
        entity.setCreatedBy(authenticatedUser); // owner = authenticated user

        ReadingGroupResponseDto response =
                mock(ReadingGroupResponseDto.class);

        when(repository.findByIdExternal(groupId))
                .thenReturn(Optional.of(entity));

        when(repository.save(entity))
                .thenReturn(entity);

        when(mapper.toResponseDto(entity))
                .thenReturn(response);

        ReadingGroupResponseDto result =
                service.updateGroup(groupId, request);

        assertEquals(response, result);

        verify(repository).save(entity);
    }

    // Verify that a reading group is deactivated successfully (soft delete)
    @Test
    void deleteGroup_shouldDeactivateGroup() {

        ReadingGroupEntity entity = new ReadingGroupEntity();
        entity.setIsActive(true);
        entity.setCreatedBy(authenticatedUser); // owner = authenticated user

        when(repository.findByIdExternal(groupId))
                .thenReturn(Optional.of(entity));

        service.deleteGroup(groupId);

        assertFalse(entity.getIsActive());
        verify(repository).save(entity);
    }
}