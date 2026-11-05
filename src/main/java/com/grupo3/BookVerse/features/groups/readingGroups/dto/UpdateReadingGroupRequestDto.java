package com.grupo3.BookVerse.features.groups.readingGroups.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateReadingGroupRequestDto(

        @NotBlank(message = "name cannot be blank")
        String name

) {
}