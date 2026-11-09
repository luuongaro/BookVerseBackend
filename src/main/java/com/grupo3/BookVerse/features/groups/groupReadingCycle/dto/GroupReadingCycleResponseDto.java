package com.grupo3.BookVerse.features.groups.groupReadingCycle.dto;

import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupReadingCycleResponseDto(

        UUID idExternal,

        UUID groupId,

        UUID bookId,

        UUID storyId,

        String editionReference,

        Integer totalChapters,

        GroupReadingCycleStatus status,

        LocalDateTime startedAt,

        LocalDateTime completedAt,

        LocalDateTime createdAt

) {
}