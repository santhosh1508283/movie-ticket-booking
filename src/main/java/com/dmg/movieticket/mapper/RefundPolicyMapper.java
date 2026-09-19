package com.dmg.movieticket.mapper;

import com.dmg.movieticket.dto.response.RefundPolicyResponse;
import com.dmg.movieticket.entity.RefundPolicy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefundPolicyMapper {

    RefundPolicyResponse toResponse(
            RefundPolicy refundPolicy
    );
}