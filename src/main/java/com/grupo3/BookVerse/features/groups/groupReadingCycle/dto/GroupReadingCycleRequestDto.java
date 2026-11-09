package com.grupo3.BookVerse.features.groups.groupReadingCycle.dto;

import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record GroupReadingCycleRequestDto(

        UUID bookId,

        UUID storyId,

        String editionReference,

        @Positive(message = "totalChapters must be greater than 0")
        Integer totalChapters

) {
}