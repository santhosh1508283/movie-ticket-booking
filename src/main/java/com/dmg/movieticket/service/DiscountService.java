package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.response.DiscountResult;

import java.math.BigDecimal;

public interface DiscountService {

    DiscountResult calculateDiscount(
            String discountCode,
            BigDecimal subtotal
    );
}