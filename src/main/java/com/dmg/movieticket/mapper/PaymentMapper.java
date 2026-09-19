package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.PaymentResponse;
import com.dmg.movieticket.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "booking.status", target = "bookingStatus")
    PaymentResponse toResponse(Payment payment);
}