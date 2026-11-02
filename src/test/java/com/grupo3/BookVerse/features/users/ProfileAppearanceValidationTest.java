package com.grupo3.BookVerse.features.users;

import com.grupo3.BookVerse.features.users.dto.ProfileAppearanceRequestDto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProfileAppearanceValidationTest {
    @Test void acceptsOnlyKnownAvatarAndCoverChoicesAndRequiresASelection() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new ProfileAppearanceRequestDto(0, null)).isEmpty());
            assertTrue(validator.validate(new ProfileAppearanceRequestDto(null, 5)).isEmpty());
            assertFalse(validator.validate(new ProfileAppearanceRequestDto(null, null)).isEmpty());
            assertFalse(validator.validate(new ProfileAppearanceRequestDto(-1, null)).isEmpty());
            assertFalse(validator.validate(new ProfileAppearanceRequestDto(null, 6)).isEmpty());
        }
    }
}
