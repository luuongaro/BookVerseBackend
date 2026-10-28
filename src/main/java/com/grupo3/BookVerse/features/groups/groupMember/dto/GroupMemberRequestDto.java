package com.grupo3.BookVerse.features.groups.groupMember.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberRequestDto {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Ingresá un correo válido")
    private String email;
}