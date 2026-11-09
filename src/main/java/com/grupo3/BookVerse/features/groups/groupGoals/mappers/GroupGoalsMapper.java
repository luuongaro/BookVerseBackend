package com.grupo3.BookVerse.features.groups.groupGoals.mappers;

import com.grupo3.BookVerse.features.groups.groupGoals.domain.GroupGoalsEntity;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupGoalsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "readingCycle", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    GroupGoalsEntity toEntity(GroupGoalsRequestDto dto);

    @Mapping(target = "groupId", source = "group.idExternal")
    @Mapping(
            target = "readingCycleId",
            source = "readingCycle.idExternal"
    )
    GroupGoalsResponseDto toResponseDto(GroupGoalsEntity entity);

    List<GroupGoalsResponseDto> toResponseDtoList(
            List<GroupGoalsEntity> entities
    );
}