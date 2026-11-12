package com.grupo3.BookVerse.features.friends;

import java.time.LocalDateTime;
import java.util.UUID;

public record FriendshipDto(UUID id, UUID userId, String username, String state, LocalDateTime createdAt) {}
