package com.grupo3.BookVerse.features.groups.groupReadingCycle.mappers;

import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleRequestDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupReadingCycleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "story", ignore = true)
    @Mapping(target = "goals", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    GroupReadingCycleEntity toEntity(GroupReadingCycleRequestDto dto);


    @Mapping(target = "groupId", source = "group.idExternal")
    @Mapping(target = "bookId", source = "book.idExternal")
    @Mapping(target = "storyId", source = "story.idExternal")
    GroupReadingCycleResponseDto toResponseDto(GroupReadingCycleEntity entity);

    List<GroupReadingCycleResponseDto> toResponseDtoList(
            List<GroupReadingCycleEntity> entities
    );
}