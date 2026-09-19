package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateDiscountCodeRequest;
import com.dmg.movieticket.dto.response.DiscountCodeResponse;

import java.util.List;

public interface DiscountCodeService {

    DiscountCodeResponse createDiscountCode(
            CreateDiscountCodeRequest request
    );

    List<DiscountCodeResponse> getAllDiscountCodes();

    DiscountCodeResponse setActive(
            Long discountCodeId,
            boolean active
    );
}