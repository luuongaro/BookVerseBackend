package com.grupo3.BookVerse.features.users.dto;

import com.grupo3.BookVerse.features.users.domain.AccountStoryAction;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record AccountDeactivationRequestDto(

        @NotNull
        AccountStoryAction storyAction,

        Map<UUID, String> creatorReplacements

) {
}
