package com.grupo3.BookVerse.features.groups.groupReadingParticipant.repository;

import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantEntity;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupReadingParticipantRepository
        extends JpaRepository<GroupReadingParticipantEntity, Long> {

    Optional<GroupReadingParticipantEntity> findByIdExternal(
            UUID idExternal
    );

    Optional<GroupReadingParticipantEntity>
    findByReadingCycle_IdExternalAndGroupMember_User_IdExternal(
            UUID cycleId,
            UUID userId
    );

    List<GroupReadingParticipantEntity>
    findByReadingCycle_IdExternalAndStatus(
            UUID cycleId,
            GroupReadingParticipantStatus status
    );

    List<GroupReadingParticipantEntity>
    findByReadingCycle_IdExternal(
            UUID cycleId
    );

    boolean existsByReadingCycle_IdExternalAndGroupMember_IdExternal(
            UUID cycleId,
            UUID groupMemberId
    );

    List<GroupReadingParticipantEntity>
    findByReadingCycle_IdExternalAndStatusIn(
            UUID cycleId,
            List<GroupReadingParticipantStatus> statuses
    );

    List<GroupReadingParticipantEntity>
    findByGroupMember_User_IdExternalAndReadingCycle_Book_IdExternal(
            UUID userId,
            UUID bookId
    );

    List<GroupReadingParticipantEntity>
    findByGroupMember_User_IdExternalAndReadingCycle_Story_IdExternal(
            UUID userId,
            UUID storyId
    );
}