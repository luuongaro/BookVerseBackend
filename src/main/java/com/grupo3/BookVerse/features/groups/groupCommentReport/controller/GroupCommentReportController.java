package com.grupo3.BookVerse.features.groups.groupCommentReport.controller;

import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportCreateRequestDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportModerationRequestDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportResponseDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.service.GroupCommentReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/group-comment-reports")
@RequiredArgsConstructor
public class GroupCommentReportController {

    private final GroupCommentReportService reportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<GroupCommentReportResponseDto>>
    getAllReports() {
        return ResponseEntity.ok(
                reportService.getAllReports()
        );
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<GroupCommentReportResponseDto>>
    getPendingReports() {
        return ResponseEntity.ok(
                reportService.getPendingReports()
        );
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<GroupCommentReportResponseDto>
    getReportById(
            @PathVariable UUID reportId
    ) {
        return ResponseEntity.ok(
                reportService.getReportById(reportId)
        );
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupCommentReportResponseDto>
    createReport(
            @Valid
            @RequestBody
            GroupCommentReportCreateRequestDto dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        reportService.createReport(dto)
                );
    }

    @PatchMapping("/{reportId}/approve")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<GroupCommentReportResponseDto>
    approveReport(
            @PathVariable UUID reportId,
            @Valid
            @RequestBody
            GroupCommentReportModerationRequestDto dto
    ) {
        return ResponseEntity.ok(
                reportService.approveReport(
                        reportId,
                        dto
                )
        );
    }

    @PatchMapping("/{reportId}/reject")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<GroupCommentReportResponseDto>
    rejectReport(
            @PathVariable UUID reportId,
            @Valid
            @RequestBody
            GroupCommentReportModerationRequestDto dto
    ) {
        return ResponseEntity.ok(
                reportService.rejectReport(
                        reportId,
                        dto
                )
        );
    }
}