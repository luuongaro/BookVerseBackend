package com.grupo3.BookVerse.features.users.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProfileAppearanceDto(int coverIndex, int avatarIndex, boolean configured) {}
