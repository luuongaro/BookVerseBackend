package com.grupo3.BookVerse.features.groups.groupProgress.service;

import com.grupo3.BookVerse.features.groups.groupProgress.dto.GroupProgressResponseDto;

import java.util.UUID;

public interface GroupProgressService {

    GroupProgressResponseDto calculateProgress(
            UUID groupId,
            UUID cycleId
    );

    double calculateAveragePercentage(
            UUID groupId,
            UUID cycleId
    );
}