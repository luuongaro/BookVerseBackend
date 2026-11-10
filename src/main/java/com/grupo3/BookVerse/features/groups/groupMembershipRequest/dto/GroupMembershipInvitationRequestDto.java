package com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GroupMembershipInvitationRequestDto(

        @NotNull(message = "targetUserId cannot be null")
        UUID targetUserId

) {
}