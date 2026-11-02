package com.grupo3.BookVerse.features.groups.groupMember.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberRequestDto {

    @Email(message = "Ingresá un correo válido")
    private String email;

    private UUID userId;

    public GroupMemberRequestDto(String email) {
        this.email = email;
    }

    @AssertTrue(message = "Indicá un usuario o un correo, no ambos")
    @JsonIgnore
    public boolean isTargetValid() {
        boolean hasEmail = email != null && !email.isBlank();
        return (userId != null) != hasEmail;
    }
}
