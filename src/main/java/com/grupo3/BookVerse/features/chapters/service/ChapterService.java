package com.grupo3.BookVerse.features.chapters.service;

import com.grupo3.BookVerse.features.chapters.dto.ChapterCreateRequestDto;
import com.grupo3.BookVerse.features.chapters.dto.ChapterResponseDto;
import com.grupo3.BookVerse.features.chapters.dto.ChapterUpdateRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ChapterService {

    ChapterResponseDto createChapter(ChapterCreateRequestDto chapterCreateRequestDto);

    Page<ChapterResponseDto> getAllChapters(Pageable pageable);

    ChapterResponseDto getChapterByIdExternal(UUID idExternal);

    Page<ChapterResponseDto> getChaptersByStoryId(UUID storyId, Pageable pageable);

    Page<ChapterResponseDto> getChaptersByStoryId(UUID storyId, Pageable pageable, boolean publishedOnly);

    ChapterResponseDto updateChapter(UUID idExternal, ChapterUpdateRequestDto chapterUpdateRequestDto);

    void deleteChapter(UUID idExternal);
}
