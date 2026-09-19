package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.response.DiscountResult;
import com.dmg.movieticket.entity.DiscountCode;
import com.dmg.movieticket.entity.DiscountType;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.repository.DiscountCodeRepository;
import com.dmg.movieticket.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountCodeRepository discountCodeRepository;

    @Override
    @Transactional(readOnly = true)
    public DiscountResult calculateDiscount(
            String discountCode,
            BigDecimal subtotal
    ) {

        if (discountCode == null || discountCode.isBlank()) {
            return new DiscountResult(
                    null,
                    BigDecimal.ZERO
            );
        }

        String normalizedCode =
                discountCode.trim().toUpperCase();

        DiscountCode discount =
                discountCodeRepository
                        .findByCodeIgnoreCase(normalizedCode)
                        .orElseThrow(() -> new ApplicationException(
                                ErrorCode.INVALID_REQUEST,
                                "Invalid discount code"
                        ));

        validateDiscount(discount, subtotal);

        BigDecimal discountAmount;

        if (discount.getDiscountType() == DiscountType.FLAT) {

            discountAmount = discount.getDiscountValue();

        } else {

            discountAmount = subtotal
                    .multiply(discount.getDiscountValue())
                    .divide(
                            new BigDecimal("100"),
                            2,
                            RoundingMode.HALF_UP
                    );

            if (discount.getMaximumDiscountAmount() != null
                    && discountAmount.compareTo(
                    discount.getMaximumDiscountAmount()
            ) > 0) {

                discountAmount =
                        discount.getMaximumDiscountAmount();
            }
        }

        /*
         * Discount can never exceed booking subtotal.
         */
        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }

        return new DiscountResult(
                discount.getCode(),
                discountAmount
        );
    }

    private void validateDiscount(
            DiscountCode discount,
            BigDecimal subtotal
    ) {

        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(discount.getActive())) {
            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Discount code is inactive"
            );
        }

        if (now.isBefore(discount.getValidFrom())
                || now.isAfter(discount.getValidUntil())) {

            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Discount code is not currently valid"
            );
        }

        if (discount.getMinimumOrderAmount() != null
                && subtotal.compareTo(
                discount.getMinimumOrderAmount()
        ) < 0) {

            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Minimum order amount for this discount is "
                            + discount.getMinimumOrderAmount()
            );
        }
    }
}