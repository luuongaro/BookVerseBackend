package com.grupo3.BookVerse.features.groups.groupReadingCycle.repository;

import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupReadingCycleRepository
        extends JpaRepository<GroupReadingCycleEntity, Long> {

    Optional<GroupReadingCycleEntity> findByIdExternal(UUID idExternal);

    List<GroupReadingCycleEntity> findByGroup_IdExternalOrderByCreatedAtDesc(
            UUID groupId
    );

    Optional<GroupReadingCycleEntity> findByGroup_IdExternalAndStatus(
            UUID groupId,
            GroupReadingCycleStatus status
    );

    boolean existsByGroup_IdExternalAndStatus(
            UUID groupId,
            GroupReadingCycleStatus status
    );

    Optional<GroupReadingCycleEntity> findByIdExternalAndGroup_IdExternal(
            UUID cycleId,
            UUID groupId
    );


}