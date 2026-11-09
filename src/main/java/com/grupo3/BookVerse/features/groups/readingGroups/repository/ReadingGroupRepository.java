package com.grupo3.BookVerse.features.groups.readingGroups.repository;

import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadingGroupRepository
        extends JpaRepository<ReadingGroupEntity, Long> {

    Optional<ReadingGroupEntity> findByIdExternal(
            UUID idExternal
    );

    List<ReadingGroupEntity> findByIsActiveTrue();

    List<ReadingGroupEntity> findByCreatedBy_IdExternalAndIsActiveTrue(
            UUID userId
    );

    List<ReadingGroupEntity>
    findDistinctByReadingCycles_Book_IdExternalAndIsActiveTrue(
            UUID bookId
    );

    List<ReadingGroupEntity>
    findDistinctByReadingCycles_Story_IdExternalAndIsActiveTrue(
            UUID storyId
    );

    boolean existsByNameIgnoreCaseAndIsActiveTrue(
            String name
    );

    boolean existsByNameIgnoreCaseAndIsActiveTrueAndIdExternalNot(
            String name,
            UUID idExternal
    );
}