package com.grupo3.BookVerse.features.groups.groupReadingParticipant.service;

import com.grupo3.BookVerse.features.groups.groupReadingParticipant.dto.GroupReadingParticipantResponseDto;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;

import java.util.List;
import java.util.UUID;

public interface GroupReadingParticipantService {

    GroupReadingParticipantResponseDto joinCycle(
            UUID groupId,
            UUID cycleId
    );

    void leaveCycle(
            UUID groupId,
            UUID cycleId
    );

    List<GroupReadingParticipantResponseDto> getParticipants(
            UUID groupId,
            UUID cycleId
    );

    void synchronizeWithReadingStatus(
            ReadingStatusEntity readingStatus
    );

    void withdrawForDeletedReadingStatus(
            ReadingStatusEntity readingStatus
    );
}