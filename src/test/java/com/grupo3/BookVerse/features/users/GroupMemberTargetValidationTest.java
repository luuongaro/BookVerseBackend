package com.grupo3.BookVerse.features.users;

import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberRequestDto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class GroupMemberTargetValidationTest {
    @Test void acceptsExactlyOneValidTarget() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(GroupMemberRequestDto.builder().userId(UUID.randomUUID()).build()).isEmpty());
            assertTrue(validator.validate(new GroupMemberRequestDto("reader@example.test")).isEmpty());
            assertFalse(validator.validate(new GroupMemberRequestDto()).isEmpty());
            assertFalse(validator.validate(new GroupMemberRequestDto("invalid-email")).isEmpty());
            assertFalse(validator.validate(GroupMemberRequestDto.builder().email("reader@example.test").userId(UUID.randomUUID()).build()).isEmpty());
        }
    }
}
