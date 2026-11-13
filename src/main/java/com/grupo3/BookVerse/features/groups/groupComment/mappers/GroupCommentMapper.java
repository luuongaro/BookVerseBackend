package com.grupo3.BookVerse.features.groups.groupComment.mappers;

import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentEntity;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentRequestDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupCommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "readingCycle", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isHidden", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    GroupCommentEntity toEntity(
            GroupCommentRequestDto dto
    );

    @Mapping(
            target = "groupId",
            source = "group.idExternal"
    )
    @Mapping(
            target = "readingCycleId",
            source = "readingCycle.idExternal"
    )
    @Mapping(
            target = "userId",
            source = "user.idExternal"
    )
    @Mapping(
            target = "userName",
            source = "user.username"
    )
    GroupCommentResponseDto toResponseDto(
            GroupCommentEntity comment
    );

    List<GroupCommentResponseDto> toResponseDtoList(
            List<GroupCommentEntity> comments
    );
}