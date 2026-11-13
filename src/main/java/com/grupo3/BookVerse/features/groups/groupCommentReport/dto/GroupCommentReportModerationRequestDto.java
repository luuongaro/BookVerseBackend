package com.grupo3.BookVerse.features.groups.groupCommentReport.dto;

import jakarta.validation.constraints.Size;

public record GroupCommentReportModerationRequestDto(

        @Size(
                max = 3000,
                message = "Resolution comment must not exceed 3000 characters"
        )
        String resolutionComment

) {
}