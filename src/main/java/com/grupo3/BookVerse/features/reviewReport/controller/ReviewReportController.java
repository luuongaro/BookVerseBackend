package com.grupo3.BookVerse.features.reviewReport.controller;

import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportCreateRequestDto;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportModerationRequestDto;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportResponseDto;
import com.grupo3.BookVerse.features.reviewReport.services.ReviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/review-reports")
@RequiredArgsConstructor
@Tag(
        name = "Review Reports",
        description = "Endpoints for managing review reports and moderation actions in BookVerse"
)
public class ReviewReportController {

    private final ReviewReportService reviewReportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Get all review reports",
            description = "Retrieves all review reports registered in the system.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review reports retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<ReviewReportResponseDto>> getAllReports() {
        return ResponseEntity.ok(reviewReportService.getAllReports());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Get pending review reports",
            description = "Retrieves all review reports that are pending moderation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending review reports retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<ReviewReportResponseDto>> getPendingReports() {
        return ResponseEntity.ok(reviewReportService.getPendingReports());
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Get review report by external id",
            description = "Retrieves a review report using its external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review report retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Review report not found", content = @Content)
    })
    public ResponseEntity<ReviewReportResponseDto> getReportById(
            @Parameter(
                    description = "External UUID of the review report",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID reportId
    ) {
        return ResponseEntity.ok(reviewReportService.getReportById(reportId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new review report",
            description = "Creates a new report for a review using the authenticated user as the reporter.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review report created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or invalid report target", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Review not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate pending report detected", content = @Content)
    })
    public ResponseEntity<ReviewReportResponseDto> createReport(
            @Valid @RequestBody ReviewReportCreateRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewReportService.createReport(dto));
    }

    @PatchMapping("/{reportId}/approve")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Approve a review report",
            description = "Approves a pending review report and takes down the associated review.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review report approved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid moderation request or report already moderated", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Review report not found", content = @Content)
    })
    public ResponseEntity<ReviewReportResponseDto> approveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportModerationRequestDto dto
    ) {
        return ResponseEntity.ok(reviewReportService.approveReport(reportId, dto));
    }

    @PatchMapping("/{reportId}/reject")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Reject a review report",
            description = "Rejects a pending review report without changing the associated review.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review report rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid moderation request or report already moderated", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Review report not found", content = @Content)
    })
    public ResponseEntity<ReviewReportResponseDto> rejectReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportModerationRequestDto dto
    ) {
        return ResponseEntity.ok(reviewReportService.rejectReport(reportId, dto));
    }

    @PatchMapping("/{reportId}/restore")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(
            summary = "Restore a taken down review",
            description = "Restores the review associated with an approved report by changing its takedown status back to ACTIVE.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Review restored successfully"),
            @ApiResponse(responseCode = "400", description = "The report was not approved, the review is deleted, or the review is not currently taken down", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Review report or associated review not found", content = @Content)
    })
    public ResponseEntity<Void> restoreReview(
            @Parameter(
                    description = "External UUID of the approved review report",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID reportId
    ) {
        reviewReportService.restoreReview(reportId);
        return ResponseEntity.noContent().build();
    }
}