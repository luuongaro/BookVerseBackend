package com.grupo3.BookVerse.features.groups.readingGroups.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReadingGroupResponseDto(

        UUID idExternal,
        UUID bookId,
        UUID storyId,
        UUID createdByUserId,
        String name,

        String editionReference,
        Integer chapterReferenceTotal,

        Boolean isActive,
        LocalDateTime createdAt

) {
}
