package com.grupo3.BookVerse.features.users.dto;

import java.util.List;
import java.util.UUID;

public record UserResponseDto(
        UUID idExternal,
        String username,
        String email,
        String status,
        List<String> roles
){}
