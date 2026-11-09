package com.grupo3.BookVerse.features.groups.groupGoals.dto;

import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record GroupGoalsRequestDto(

        @NotNull(message = "goalType cannot be null")
        GoalType goalType,

        @NotNull(message = "targetProgress cannot be null")
        @Positive(message = "targetProgress must be greater than 0")
        Integer targetProgress,

        @NotNull(message = "targetDate cannot be null")
        @Future(message = "targetDate must be a future date")
        LocalDate targetDate

) {
}