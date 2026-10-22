package com.grupo3.BookVerse.features.groups.groupGoals.service;

import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalStatusRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsResponseDto;

import java.util.List;
import java.util.UUID;

public interface GroupGoalsService {


    GroupGoalsResponseDto createGoal(UUID groupId, GroupGoalsRequestDto requestDto);

    GroupGoalsResponseDto getGoalByIdExternal(UUID goalId);

    List<GroupGoalsResponseDto> getGoalsByGroupId(UUID groupId);

    GroupGoalsResponseDto getActiveGoalByGroupId(UUID groupId);

    GroupGoalsResponseDto changeStatus(UUID goalId, GroupGoalStatusRequestDto requestDto);

    void cancelGoal(UUID goalId);

    // Additional CRUD methods
    GroupGoalsResponseDto save(GroupGoalsRequestDto requestDto);

    GroupGoalsResponseDto findById(UUID goalId);

    void delete(UUID goalId);

    List<GroupGoalsResponseDto> findAll();

    GroupGoalsResponseDto findByGroupId(UUID groupId);

}
