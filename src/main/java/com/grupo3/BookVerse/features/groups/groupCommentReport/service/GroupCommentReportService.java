package com.grupo3.BookVerse.features.groups.groupCommentReport.service;

import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportCreateRequestDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportModerationRequestDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportResponseDto;

import java.util.List;
import java.util.UUID;

public interface GroupCommentReportService {

    List<GroupCommentReportResponseDto> getAllReports();

    List<GroupCommentReportResponseDto> getPendingReports();

    GroupCommentReportResponseDto getReportById(
            UUID reportId
    );

    GroupCommentReportResponseDto createReport(
            GroupCommentReportCreateRequestDto dto
    );

    GroupCommentReportResponseDto approveReport(
            UUID reportId,
            GroupCommentReportModerationRequestDto dto
    );

    GroupCommentReportResponseDto rejectReport(
            UUID reportId,
            GroupCommentReportModerationRequestDto dto
    );
}