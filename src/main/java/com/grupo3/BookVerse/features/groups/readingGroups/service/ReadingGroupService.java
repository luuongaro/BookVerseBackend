package com.grupo3.BookVerse.features.groups.readingGroups.service;

import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupDiscoveryResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.UpdateReadingGroupRequestDto;

import java.util.List;
import java.util.UUID;

public interface ReadingGroupService {

    ReadingGroupResponseDto createGroup(ReadingGroupRequestDto dto);

    List<ReadingGroupResponseDto> getAllGroups();

    ReadingGroupResponseDto getGroupByIdExternal(UUID idExternal);

    List<ReadingGroupResponseDto> getGroupsByBookIdExternal(UUID bookId);

    List<ReadingGroupResponseDto> getGroupsByStoryIdExternal(UUID storyId);

    List<ReadingGroupResponseDto> getGroupsByUserIdExternal(UUID userId);

    ReadingGroupResponseDto updateGroup(UUID idExternal, UpdateReadingGroupRequestDto dto);

    void deleteGroup(UUID idExternal);

    List<ReadingGroupDiscoveryResponseDto> discoverGroups();

    List<ReadingGroupDiscoveryResponseDto> searchGroups(
            String query
    );
}