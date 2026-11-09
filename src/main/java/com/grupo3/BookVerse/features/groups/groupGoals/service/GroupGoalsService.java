package com.grupo3.BookVerse.features.groups.groupGoals.service;

import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalStatusRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsResponseDto;

import java.util.List;
import java.util.UUID;

public interface GroupGoalsService {

    GroupGoalsResponseDto createGoal(
            UUID groupId,
            UUID cycleId,
            GroupGoalsRequestDto requestDto
    );

    GroupGoalsResponseDto getGoalByIdExternal(
            UUID groupId,
            UUID cycleId,
            UUID goalId
    );

    List<GroupGoalsResponseDto> getGoalsByCycleId(
            UUID groupId,
            UUID cycleId
    );

    GroupGoalsResponseDto getActiveGoalByCycleId(
            UUID groupId,
            UUID cycleId
    );

    GroupGoalsResponseDto changeStatus(
            UUID groupId,
            UUID cycleId,
            UUID goalId,
            GroupGoalStatusRequestDto requestDto
    );

    void cancelGoal(
            UUID groupId,
            UUID cycleId,
            UUID goalId
    );
}