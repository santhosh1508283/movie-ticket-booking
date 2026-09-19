package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.DiscountCodeResponse;
import com.dmg.movieticket.entity.DiscountCode;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DiscountCodeMapper {

    DiscountCodeResponse toResponse(DiscountCode discountCode);
}