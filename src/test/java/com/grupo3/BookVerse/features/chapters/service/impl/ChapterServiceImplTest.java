package com.grupo3.BookVerse.features.chapters.service.impl;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import com.grupo3.BookVerse.features.chapters.dto.ChapterResponseDto;
import com.grupo3.BookVerse.features.chapters.dto.ChapterUpdateRequestDto;
import com.grupo3.BookVerse.features.chapters.mappers.ChapterMapper;
import com.grupo3.BookVerse.features.chapters.repository.ChapterRepository;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChapterServiceImplTest {

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private ChapterMapper chapterMapper;

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private ChapterServiceImpl chapterService;


    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // verifies that the service correctly retrieves all non-deleted chapters and maps them to dtos
    @Test
    void readingModeReturnsOnlyPublishedChaptersEvenForTheAuthor() {
        var author = new UserEntity();
        author.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(author, null, List.of()));
        var storyId = UUID.randomUUID();
        var story = StoryEntity.builder().id(2L).author(author).build();
        var pageable = PageRequest.of(0, 20);
        when(storyRepository.findByIdExternalAndDeletedFalse(storyId)).thenReturn(Optional.of(story));
        when(chapterRepository.findByStoryIdAndDeletedFalseAndPublishedTrueOrderByChapterNumberAsc(2L, pageable))
                .thenReturn(Page.empty(pageable));

        assertTrue(chapterService.getChaptersByStoryId(storyId, pageable, true).isEmpty());
        verify(chapterRepository, never()).findByStoryIdAndDeletedFalseOrderByChapterNumberAsc(any(), any());
    }

    @Test
    void shouldReturnAllChapters() {

        Pageable pageable = PageRequest.of(0, 10);

        ChapterEntity chapter =
                ChapterEntity.builder()
                        .title("Capítulo 1")
                        .deleted(false)
                        .build();

        ChapterResponseDto dto =
                ChapterResponseDto.builder()
                        .title("Capítulo 1")
                        .build();

        Page<ChapterEntity> chapterPage =
                new PageImpl<>(List.of(chapter));

        when(chapterRepository
                .findAllByDeletedFalseOrderByCreatedAtDesc(pageable))
                .thenReturn(chapterPage);

        when(chapterMapper.toResponseDto(chapter))
                .thenReturn(dto);

        Page<ChapterResponseDto> result =
                chapterService.getAllChapters(pageable);

        assertNotNull(result);

        assertEquals(
                1,
                result.getContent().size()
        );

        assertEquals(
                "Capítulo 1",
                result.getContent().get(0).getTitle()
        );

        verify(chapterRepository)
                .findAllByDeletedFalseOrderByCreatedAtDesc(pageable);

        verify(chapterMapper)
                .toResponseDto(chapter);
    }

    //Verifies that the service correctly retrieves a chapter by id
    @Test
    void shouldReturnChapterByIdExternal() {
        var reader = new UserEntity();
        reader.setId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(reader, null, List.of()));


        UUID id = UUID.randomUUID();

        StoryEntity story = new StoryEntity();
        story.setDeleted(false);
        story.setTitle("My Story");

        ChapterEntity chapter =
                ChapterEntity.builder()
                        .idExternal(id)
                        .title("Chapter One")
                        .published(true)
                        .story(story)
                        .deleted(false)
                        .build();

        ChapterResponseDto dto =
                ChapterResponseDto.builder()
                        .title("Chapter One")
                        .build();

        when(chapterRepository
                .findByIdExternalAndDeletedFalse(id))
                .thenReturn(Optional.of(chapter));

        when(chapterMapper.toResponseDto(chapter))
                .thenReturn(dto);

        ChapterResponseDto result =
                chapterService.getChapterByIdExternal(id);

        assertNotNull(result);

        assertEquals(
                "Chapter One",
                result.getTitle()
        );

        verify(chapterRepository)
                .findByIdExternalAndDeletedFalse(id);

        verify(chapterMapper)
                .toResponseDto(chapter);
    }

    //Verifies that the service throws an exception when the chapter doesn't exist
    @Test
    void shouldThrowExceptionWhenChapterNotFound() {

        UUID id = UUID.randomUUID();

        when(chapterRepository
                .findByIdExternalAndDeletedFalse(id))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> chapterService.getChapterByIdExternal(id)
        );

        verify(chapterRepository)
                .findByIdExternalAndDeletedFalse(id);
    }

    //Verifies that the service correctly updates an existing chapter
    @Test
    void shouldUpdateChapter() {

        UUID id = UUID.randomUUID();

        UserEntity user = new UserEntity();
        user.setId(1L);

        Authentication authentication =
                mock(Authentication.class);

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(authentication.getPrincipal())
                .thenReturn(user);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder
                .setContext(securityContext);

        StoryEntity story = new StoryEntity();
        story.setDeleted(false);

        UserEntity author = new UserEntity();
        author.setId(1L);

        story.setAuthor(author);

        ChapterEntity chapter =
                ChapterEntity.builder()
                        .idExternal(id)
                        .title("Old Title")
                        .content("Old Content")
                        .story(story)
                        .deleted(false)
                        .build();

        ChapterUpdateRequestDto dto =
                ChapterUpdateRequestDto.builder()
                        .title("New Title")
                        .content("New Content")
                        .published(true)
                        .build();

        ChapterResponseDto response =
                ChapterResponseDto.builder()
                        .title("New Title")
                        .build();

        when(chapterRepository
                .findByIdExternalAndDeletedFalse(id))
                .thenReturn(Optional.of(chapter));

        when(chapterRepository.save(chapter))
                .thenReturn(chapter);

        when(chapterMapper.toResponseDto(chapter))
                .thenReturn(response);

        ChapterResponseDto result =
                chapterService.updateChapter(id, dto);

        assertNotNull(result);

        assertEquals(
                "New Title",
                result.getTitle()
        );

        assertEquals(
                "New Title",
                chapter.getTitle()
        );

        assertEquals(
                "New Content",
                chapter.getContent()
        );

        assertTrue(chapter.isPublished());

        verify(chapterRepository)
                .save(chapter);

        verify(chapterMapper)
                .toResponseDto(chapter);
    }

    //Verifies that the service correctly performs a soft delete on a chapter.
    @Test
    void shouldDeleteChapter() {

        UUID id = UUID.randomUUID();

        UserEntity user = new UserEntity();
        user.setId(1L);

        Authentication authentication =
                mock(Authentication.class);

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(authentication.getPrincipal())
                .thenReturn(user);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder
                .setContext(securityContext);

        StoryEntity story = new StoryEntity();
        story.setDeleted(false);

        UserEntity author = new UserEntity();
        author.setId(1L);

        story.setAuthor(author);

        ChapterEntity chapter =
                ChapterEntity.builder()
                        .idExternal(id)
                        .story(story)
                        .deleted(false)
                        .build();

        when(chapterRepository
                .findByIdExternalAndDeletedFalse(id))
                .thenReturn(Optional.of(chapter));

        when(chapterRepository.save(chapter))
                .thenReturn(chapter);

        chapterService.deleteChapter(id);

        assertTrue(chapter.isDeleted());

        verify(chapterRepository)
                .save(chapter);
    }
}
