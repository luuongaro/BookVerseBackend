package com.grupo3.BookVerse.features.groups.groupReadingCycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateGroupReadingCycleRequestDto(

        @NotBlank(message = "editionReference cannot be blank")
        String editionReference,

        @NotNull(message = "totalChapters cannot be null")
        @Positive(message = "totalChapters must be greater than 0")
        Integer totalChapters

) {
}