package com.grupo3.BookVerse.features.users.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProfileAppearanceRequestDto(@Min(0) @Max(5) Integer coverIndex,
                                           @Min(0) @Max(5) Integer avatarIndex) {
    @AssertTrue(message = "Seleccioná una portada o un avatar")
    @JsonIgnore
    public boolean isSelectionPresent() { return coverIndex != null || avatarIndex != null; }
}
