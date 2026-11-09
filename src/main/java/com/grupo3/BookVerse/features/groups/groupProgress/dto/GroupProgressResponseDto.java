package com.grupo3.BookVerse.features.groups.groupProgress.dto;

import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record GroupProgressResponseDto(

        UUID groupId,

        UUID readingCycleId,

        GoalType goalType,

        Double currentProgress,

        Integer targetProgress,

        Boolean achieved

) {
}