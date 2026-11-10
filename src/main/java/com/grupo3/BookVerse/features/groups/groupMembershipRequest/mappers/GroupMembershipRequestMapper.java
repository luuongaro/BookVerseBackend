package com.grupo3.BookVerse.features.groups.groupMembershipRequest.mappers;

import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestEntity;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto.GroupMembershipRequestResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMembershipRequestMapper {

    @Mapping(target = "groupId", source = "group.idExternal")
    @Mapping(target = "targetUserId", source = "targetUser.idExternal")
    @Mapping(target = "createdById", source = "createdBy.idExternal")
    GroupMembershipRequestResponseDto toResponseDto(
            GroupMembershipRequestEntity entity
    );

    List<GroupMembershipRequestResponseDto> toResponseDtoList(
            List<GroupMembershipRequestEntity> entities
    );
}