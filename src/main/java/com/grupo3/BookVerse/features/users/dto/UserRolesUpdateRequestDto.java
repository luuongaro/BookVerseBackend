package com.grupo3.BookVerse.features.users.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

public record UserRolesUpdateRequestDto(
        @NotEmpty(message = "Select at least one role")
        Set<UUID> roleIds
) {
}