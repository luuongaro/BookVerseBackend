package com.grupo3.BookVerse.features.groups.groupReadingParticipant.mappers;

import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantEntity;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.dto.GroupReadingParticipantResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupReadingParticipantMapper {

    @Mapping(
            target = "readingCycleId",
            source = "readingCycle.idExternal"
    )
    @Mapping(
            target = "groupId",
            source = "readingCycle.group.idExternal"
    )
    @Mapping(
            target = "groupMemberId",
            source = "groupMember.idExternal"
    )
    @Mapping(
            target = "userId",
            source = "groupMember.user.idExternal"
    )
    @Mapping(
            target = "userName",
            source = "groupMember.user.profileUsername"
    )
    GroupReadingParticipantResponseDto toResponseDto(
            GroupReadingParticipantEntity entity
    );

    List<GroupReadingParticipantResponseDto> toResponseDtoList(
            List<GroupReadingParticipantEntity> entities
    );
}