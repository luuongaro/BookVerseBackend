package com.grupo3.BookVerse.features.groups.readingGroups.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UpdateReadingGroupRequestDto(

        @NotBlank(message = "name cannot be blank")
        String name,

        @Positive(message = "totalChapters must be greater than 0")
        Integer totalChapters

) {
}