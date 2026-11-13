package com.grupo3.BookVerse.features.groups.groupCommentReport.mappers;

import com.grupo3.BookVerse.features.groups.groupCommentReport.domain.GroupCommentReportEntity;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportCreateRequestDto;
import com.grupo3.BookVerse.features.groups.groupCommentReport.dto.GroupCommentReportResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface GroupCommentReportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "comment", ignore = true)
    @Mapping(target = "reporterUser", ignore = true)
    @Mapping(target = "moderatorUser", ignore = true)
    @Mapping(target = "resolutionComment", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    GroupCommentReportEntity toEntity(
            GroupCommentReportCreateRequestDto dto
    );

    @Mapping(
            source = "idExternal",
            target = "reportId"
    )
    @Mapping(
            source = "comment.idExternal",
            target = "commentId"
    )
    @Mapping(
            source = "comment.group.idExternal",
            target = "groupId"
    )
    @Mapping(
            source = "comment.readingCycle.idExternal",
            target = "readingCycleId"
    )
    @Mapping(
            source = "comment.user.idExternal",
            target = "commentAuthorId"
    )
    @Mapping(
            source = "comment.user.profileUsername",
            target = "commentAuthorUsername"
    )
    @Mapping(
            source = "comment.content",
            target = "commentContent"
    )
    @Mapping(
            source = "comment.progressType",
            target = "commentProgressType"
    )
    @Mapping(
            source = "comment.progressValue",
            target = "commentProgressValue"
    )
    @Mapping(
            source = "reporterUser.idExternal",
            target = "reporterUserId"
    )
    @Mapping(
            source = "reporterUser.profileUsername",
            target = "reporterUsername"
    )
    @Mapping(
            source = "moderatorUser.idExternal",
            target = "moderatorUserId"
    )
    GroupCommentReportResponseDto toResponseDto(
            GroupCommentReportEntity report
    );

    List<GroupCommentReportResponseDto> toResponseDtoList(
            List<GroupCommentReportEntity> reports
    );
}