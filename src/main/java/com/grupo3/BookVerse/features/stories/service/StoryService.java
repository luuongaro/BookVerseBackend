package com.grupo3.BookVerse.features.stories.service;

import com.grupo3.BookVerse.features.stories.dto.StoryDetailResponseDto;
import com.grupo3.BookVerse.features.stories.dto.StoryRequestDto;
import com.grupo3.BookVerse.features.stories.dto.StoryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StoryService {


    StoryResponseDto createStory(StoryRequestDto storyRequestDto);

    Page<StoryResponseDto> getAllStories(Pageable pageable);

    Page<StoryResponseDto> getStoriesToExplore(Pageable pageable);

    StoryDetailResponseDto getStoryByIdExternal(UUID idExternal);

    Page<StoryResponseDto> getStoriesByAuthorId(UUID authorId, Pageable pageable);

    StoryResponseDto updateStory(UUID idExternal, StoryRequestDto storyRequestDto);

    void deleteStory(UUID idExternal);
}
