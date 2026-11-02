package com.grupo3.BookVerse.features.users.dto;

import java.util.UUID;

public record UserProfileResponseDto(UUID idExternal, String username, int avatarIndex) {
    public UserProfileResponseDto(UUID idExternal, String username) { this(idExternal, username, 0); }
}
