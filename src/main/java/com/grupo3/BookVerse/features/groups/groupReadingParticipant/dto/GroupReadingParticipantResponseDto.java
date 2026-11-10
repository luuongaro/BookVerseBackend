package com.grupo3.BookVerse.features.groups.groupReadingParticipant.dto;

import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupReadingParticipantResponseDto(

        UUID idExternal,

        UUID readingCycleId,

        UUID groupId,

        UUID groupMemberId,

        UUID userId,

        String userName,

        GroupReadingParticipantStatus status,

        LocalDateTime joinedAt,

        LocalDateTime completedAt

) {
}