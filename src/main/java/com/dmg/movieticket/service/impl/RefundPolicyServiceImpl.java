package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateRefundPolicyRequest;
import com.dmg.movieticket.dto.response.RefundPolicyResponse;
import com.dmg.movieticket.entity.RefundPolicy;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.mapper.RefundPolicyMapper;
import com.dmg.movieticket.repository.RefundPolicyRepository;
import com.dmg.movieticket.service.RefundPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundPolicyServiceImpl
        implements RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;
    private final RefundPolicyMapper refundPolicyMapper;

    @Override
    @Transactional
    public RefundPolicyResponse createPolicy(
            CreateRefundPolicyRequest request
    ) {

        if (refundPolicyRepository
                .existsByHoursBeforeShow(
                        request.hoursBeforeShow()
                )) {

            throw new ApplicationException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Refund policy already exists for "
                            + request.hoursBeforeShow()
                            + " hours before show"
            );
        }

        RefundPolicy policy =
                RefundPolicy.builder()
                        .hoursBeforeShow(
                                request.hoursBeforeShow()
                        )
                        .refundPercentage(
                                request.refundPercentage()
                        )
                        .active(true)
                        .build();

        return refundPolicyMapper.toResponse(
                refundPolicyRepository.save(policy)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundPolicyResponse> getAllPolicies() {

        return refundPolicyRepository
                .findAll()
                .stream()
                .map(refundPolicyMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RefundPolicyResponse setActive(
            Long policyId,
            boolean active
    ) {

        RefundPolicy policy =
                refundPolicyRepository
                        .findById(policyId)
                        .orElseThrow(() ->
                                new ApplicationException(
                                        ErrorCode.RESOURCE_NOT_FOUND,
                                        "Refund policy not found with id: "
                                                + policyId
                                )
                        );

        policy.setActive(active);

        return refundPolicyMapper.toResponse(
                refundPolicyRepository.save(policy)
        );
    }
}