package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateDiscountCodeRequest;
import com.dmg.movieticket.dto.response.DiscountCodeResponse;
import com.dmg.movieticket.entity.DiscountCode;
import com.dmg.movieticket.entity.DiscountType;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.DiscountCodeMapper;
import com.dmg.movieticket.repository.DiscountCodeRepository;
import com.dmg.movieticket.service.DiscountCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountCodeServiceImpl
        implements DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountCodeMapper discountCodeMapper;

    @Override
    @Transactional
    public DiscountCodeResponse createDiscountCode(
            CreateDiscountCodeRequest request
    ) {

        String code =
                request.code()
                        .trim()
                        .toUpperCase();

        if (discountCodeRepository
                .existsByCodeIgnoreCase(code)) {

            throw new ApplicationException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Discount code already exists: " + code
            );
        }

        if (!request.validUntil()
                .isAfter(request.validFrom())) {

            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "validUntil must be after validFrom"
            );
        }

        if (request.discountType()
                == DiscountType.PERCENTAGE
                && request.discountValue()
                .compareTo(new BigDecimal("100")) > 0) {

            throw new ApplicationException(
                    ErrorCode.INVALID_REQUEST,
                    "Percentage discount cannot exceed 100"
            );
        }

        DiscountCode discountCode =
                DiscountCode.builder()
                        .code(code)
                        .discountType(
                                request.discountType()
                        )
                        .discountValue(
                                request.discountValue()
                        )
                        .minimumOrderAmount(
                                request.minimumOrderAmount()
                        )
                        .maximumDiscountAmount(
                                request.maximumDiscountAmount()
                        )
                        .validFrom(
                                request.validFrom()
                        )
                        .validUntil(
                                request.validUntil()
                        )
                        .active(true)
                        .build();

        DiscountCode saved =
                discountCodeRepository.save(
                        discountCode
                );

        return discountCodeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscountCodeResponse> getAllDiscountCodes() {

        return discountCodeRepository
                .findAll()
                .stream()
                .map(discountCodeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DiscountCodeResponse setActive(
            Long discountCodeId,
            boolean active
    ) {

        DiscountCode discountCode =
                discountCodeRepository
                        .findById(discountCodeId)
                        .orElseThrow(() ->
                                new ApplicationException(
                                        ErrorCode.RESOURCE_NOT_FOUND,
                                        "Discount code not found with id: "
                                                + discountCodeId
                                )
                        );

        discountCode.setActive(active);

        return discountCodeMapper.toResponse(
                discountCodeRepository.save(discountCode)
        );
    }
}