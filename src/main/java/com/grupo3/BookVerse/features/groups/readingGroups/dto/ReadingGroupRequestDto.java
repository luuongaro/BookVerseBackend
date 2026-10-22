package com.grupo3.BookVerse.features.groups.readingGroups.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ReadingGroupRequestDto(

        UUID bookId,

        UUID storyId,

        @NotBlank(message = "name cannot be blank")
        String name,

        Boolean isActive

) {
}