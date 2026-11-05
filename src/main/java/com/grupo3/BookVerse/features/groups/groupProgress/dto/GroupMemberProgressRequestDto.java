package com.grupo3.BookVerse.features.groups.groupProgress.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GroupMemberProgressRequestDto(

        @NotNull(message = "chapterProgress cannot be null")
        @Positive(message = "chapterProgress must be greater than 0")
        Integer chapterProgress

) {
}