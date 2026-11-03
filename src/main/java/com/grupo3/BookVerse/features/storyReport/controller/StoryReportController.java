package com.grupo3.BookVerse.features.storyReport.controller;

import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportCreateRequestDto;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportModerationRequestDto;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportResponseDto;
import com.grupo3.BookVerse.features.storyReport.services.StoryReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/story-reports")
@RequiredArgsConstructor
public class StoryReportController {

    private final StoryReportService storyReportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<StoryReportResponseDto>> getAllReports() {
        return ResponseEntity.ok(storyReportService.getAllReports());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<StoryReportResponseDto>> getPendingReports() {
        return ResponseEntity.ok(storyReportService.getPendingReports());
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<StoryReportResponseDto> getReportById(
            @PathVariable UUID reportId
    ) {
        return ResponseEntity.ok(storyReportService.getReportById(reportId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StoryReportResponseDto> createReport(
            @Valid @RequestBody StoryReportCreateRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storyReportService.createReport(dto));
    }

    @PatchMapping("/{reportId}/approve")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<StoryReportResponseDto> approveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody StoryReportModerationRequestDto dto
    ) {
        return ResponseEntity.ok(
                storyReportService.approveReport(reportId, dto)
        );
    }

    @PatchMapping("/{reportId}/reject")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<StoryReportResponseDto> rejectReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody StoryReportModerationRequestDto dto
    ) {
        return ResponseEntity.ok(
                storyReportService.rejectReport(reportId, dto)
        );
    }
}
