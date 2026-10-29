package com.grupo3.BookVerse.features.tips.mappers;

import com.grupo3.BookVerse.features.tips.domain.TipEntity;
import com.grupo3.BookVerse.features.tips.dto.TipRequestDto;
import com.grupo3.BookVerse.features.tips.dto.TipResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TipMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    @Mapping(target = "story", ignore = true)
    @Mapping(target = "payment", ignore = true)
    TipEntity toEntity(TipRequestDto dto);

    @Mapping(target = "senderUserId", source = "sender.idExternal")
    @Mapping(target = "receiverUserId", source = "receiver.idExternal")
    @Mapping(target = "storyId", source = "story.idExternal")
    @Mapping(target = "paymentId", source = "payment.idExternal")
    @Mapping(target = "paymentStatus", source = "payment.status")
    TipResponseDto toResponseDto(TipEntity tipEntity);

    List<TipResponseDto> toResponseDtoList(List<TipEntity> tips);
}
