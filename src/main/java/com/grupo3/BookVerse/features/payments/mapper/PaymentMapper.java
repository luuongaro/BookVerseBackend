package com.grupo3.BookVerse.features.payments.mapper;

import com.grupo3.BookVerse.features.payments.domain.PaymentEntity;
import com.grupo3.BookVerse.features.payments.dto.PaymentResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PaymentMapper {

    @Mapping(source = "idExternal", target = "paymentId")
    PaymentResponseDto toResponseDto(PaymentEntity payment);

    List<PaymentResponseDto> toResponseDtoList(
            List<PaymentEntity> payments
    );
}
