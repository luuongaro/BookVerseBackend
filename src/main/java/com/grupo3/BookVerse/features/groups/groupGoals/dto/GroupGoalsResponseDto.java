package com.grupo3.BookVerse.features.groups.groupGoals.dto;

import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalStatus;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalType;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record GroupGoalsResponseDto(

        UUID idExternal,

        UUID groupId,

        GoalType goalType,

        GoalStatus status,

        Integer targetProgress,

        LocalDate targetDate,

        LocalDateTime updatedAt

) {
}
