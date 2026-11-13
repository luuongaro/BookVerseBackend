package com.grupo3.BookVerse.features.groups.groupCommentReport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GroupCommentReportCreateRequestDto(

        @NotNull(message = "Comment id is required")
        UUID commentId,

        @NotBlank(message = "Reason cannot be empty")
        @Size(
                min = 5,
                max = 1000,
                message = "Reason must be between 5 and 1000 characters"
        )
        String reason,

        @Size(
                max = 3000,
                message = "Details must not exceed 3000 characters"
        )
        String details

) {
}