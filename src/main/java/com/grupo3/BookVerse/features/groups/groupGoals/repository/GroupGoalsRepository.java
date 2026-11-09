package com.grupo3.BookVerse.features.groups.groupGoals.repository;

import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalStatus;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalType;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GroupGoalsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupGoalsRepository extends JpaRepository<GroupGoalsEntity, Long> {


    Optional<GroupGoalsEntity> findByIdExternal(UUID idExternal);

    List<GroupGoalsEntity> findByGroup_IdExternalOrderByUpdatedAtDesc(UUID groupId);

    Optional<GroupGoalsEntity> findByGroup_IdExternalAndStatus(UUID groupId, GoalStatus status);

    boolean existsByGroup_IdExternalAndStatus(UUID groupId, GoalStatus status);

    Optional<GroupGoalsEntity> findTopByGroup_IdExternalOrderByUpdatedAtDesc(UUID groupId);

    List<GroupGoalsEntity> findByReadingCycle_IdExternalOrderByUpdatedAtDesc(
            UUID cycleId
    );

    Optional<GroupGoalsEntity> findByReadingCycle_IdExternalAndStatus(
            UUID cycleId,
            GoalStatus status
    );

    boolean existsByReadingCycle_IdExternalAndStatus(
            UUID cycleId,
            GoalStatus status
    );

    Optional<GroupGoalsEntity> findTopByReadingCycle_IdExternalOrderByUpdatedAtDesc(
            UUID cycleId
    );

    List<GroupGoalsEntity> findByReadingCycle_IdExternalAndGoalTypeAndStatusOrderByTargetProgressDesc(
            UUID cycleId,
            GoalType goalType,
            GoalStatus status
    );

    Optional<GroupGoalsEntity>
    findByIdExternalAndReadingCycle_IdExternalAndReadingCycle_Group_IdExternal(
            UUID goalId,
            UUID cycleId,
            UUID groupId
    );

}
