package com.grupo3.BookVerse.features.status.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ShelfOrderRequest(@NotEmpty @Size(max = 1000) List<@NotNull UUID> readingStatusIds) {}
