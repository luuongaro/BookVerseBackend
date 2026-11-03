package com.grupo3.BookVerse.features.storyReport.services;

import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportCreateRequestDto;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportModerationRequestDto;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportResponseDto;

import java.util.List;
import java.util.UUID;

public interface StoryReportService {

    List<StoryReportResponseDto> getAllReports();

    List<StoryReportResponseDto> getPendingReports();

    StoryReportResponseDto getReportById(UUID reportId);

    StoryReportResponseDto createReport(StoryReportCreateRequestDto dto);

    StoryReportResponseDto approveReport(
            UUID reportId,
            StoryReportModerationRequestDto dto
    );

    StoryReportResponseDto rejectReport(
            UUID reportId,
            StoryReportModerationRequestDto dto
    );
}
