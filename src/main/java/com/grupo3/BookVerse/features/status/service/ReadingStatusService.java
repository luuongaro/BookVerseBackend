package com.grupo3.BookVerse.features.status.service;


import com.grupo3.BookVerse.features.status.dto.ReadingStatusRequestDto;
import com.grupo3.BookVerse.features.status.dto.ReadingStatusResponseDto;

import java.util.List;
import java.util.UUID;

public interface ReadingStatusService {

    ReadingStatusResponseDto createReadingStatus(
            ReadingStatusRequestDto requestDto
    );

    ReadingStatusResponseDto getReadingStatusByIdExternal(
            UUID idExternal
    );

    List<ReadingStatusResponseDto> getAllReadingStatuses();

    List<ReadingStatusResponseDto> getMyReadingStatuses();

    List<ReadingStatusResponseDto> getReadingStatusesByUser(
            UUID userId
    );

    ReadingStatusResponseDto updateReadingStatus(
            UUID idExternal,
            ReadingStatusRequestDto requestDto
    );

    void deleteReadingStatus(
            UUID idExternal
    );
}