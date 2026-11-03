package com.grupo3.BookVerse.features.storyReport.mapper;

import com.grupo3.BookVerse.features.storyReport.domain.StoryReportEntity;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportCreateRequestDto;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface StoryReportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "story", ignore = true)
    @Mapping(target = "reporterUser", ignore = true)
    @Mapping(target = "moderatorUser", ignore = true)
    @Mapping(target = "resolutionComment", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    StoryReportEntity toEntity(StoryReportCreateRequestDto dto);

    @Mapping(source = "idExternal", target = "reportId")
    @Mapping(source = "story.idExternal", target = "storyId")
    @Mapping(source = "story.title", target = "storyTitle")
    @Mapping(source = "story.author.profileUsername", target = "storyAuthorUsername")
    @Mapping(source = "reporterUser.idExternal", target = "reporterUserId")
    @Mapping(source = "reporterUser.profileUsername", target = "reporterUsername")
    @Mapping(source = "moderatorUser.idExternal", target = "moderatorUserId")
    StoryReportResponseDto toResponseDto(StoryReportEntity report);

    List<StoryReportResponseDto> toResponseListDto(
            List<StoryReportEntity> reports
    );
}
