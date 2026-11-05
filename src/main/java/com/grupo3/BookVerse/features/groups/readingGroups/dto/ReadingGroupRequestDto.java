package com.grupo3.BookVerse.features.groups.readingGroups.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReadingGroupRequestDto(

        UUID bookId,

        UUID storyId,

        @NotBlank(message = "name cannot be blank")
        String name,

        String editionReference,

        @Positive(message = "totalChapters must be greater than 0")
        Integer totalChapters,

        Boolean isActive

) {
}