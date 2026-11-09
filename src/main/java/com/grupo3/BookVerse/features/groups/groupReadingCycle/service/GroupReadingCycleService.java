package com.grupo3.BookVerse.features.groups.groupReadingCycle.service;

import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleRequestDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleResponseDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.UpdateGroupReadingCycleRequestDto;

import java.util.List;
import java.util.UUID;

public interface GroupReadingCycleService {

    GroupReadingCycleResponseDto createCycle(
            UUID groupId,
            GroupReadingCycleRequestDto dto
    );

    GroupReadingCycleResponseDto getCycleByIdExternal(
            UUID groupId,
            UUID cycleId
    );

    List<GroupReadingCycleResponseDto> getCyclesByGroupId(UUID groupId);

    GroupReadingCycleResponseDto getActiveCycleByGroupId(UUID groupId);

    GroupReadingCycleResponseDto updateBookReference(
            UUID groupId,
            UUID cycleId,
            UpdateGroupReadingCycleRequestDto dto
    );

    GroupReadingCycleResponseDto startCycle(
            UUID groupId,
            UUID cycleId
    );

    GroupReadingCycleResponseDto completeCycle(
            UUID groupId,
            UUID cycleId
    );

    GroupReadingCycleResponseDto cancelCycle(
            UUID groupId,
            UUID cycleId
    );
}