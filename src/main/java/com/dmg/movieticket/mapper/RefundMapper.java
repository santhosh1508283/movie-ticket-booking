package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.RefundResponse;
import com.dmg.movieticket.entity.Refund;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefundMapper {

    @Mapping(source = "id", target = "refundId")
    @Mapping(source = "booking.id", target = "bookingId")
    RefundResponse toResponse(Refund refund);
}