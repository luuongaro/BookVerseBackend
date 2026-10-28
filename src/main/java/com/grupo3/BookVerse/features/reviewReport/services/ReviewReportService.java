package com.grupo3.BookVerse.features.reviewReport.services;

import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportCreateRequestDto;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportModerationRequestDto;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportResponseDto;

import java.util.List;
import java.util.UUID;

public interface ReviewReportService {

    List<ReviewReportResponseDto> getAllReports();

    List<ReviewReportResponseDto> getPendingReports();

    ReviewReportResponseDto getReportById(UUID reportId);

    ReviewReportResponseDto createReport(ReviewReportCreateRequestDto dto);

    ReviewReportResponseDto approveReport(UUID reportId, ReviewReportModerationRequestDto dto);

    ReviewReportResponseDto rejectReport(UUID reportId, ReviewReportModerationRequestDto dto);

    void restoreReview(UUID reportId);}