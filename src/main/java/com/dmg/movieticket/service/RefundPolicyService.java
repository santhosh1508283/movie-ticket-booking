package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.CreateRefundPolicyRequest;
import com.dmg.movieticket.dto.response.RefundPolicyResponse;

import java.util.List;

public interface RefundPolicyService {

    RefundPolicyResponse createPolicy(
            CreateRefundPolicyRequest request
    );

    List<RefundPolicyResponse> getAllPolicies();

    RefundPolicyResponse setActive(
            Long policyId,
            boolean active
    );
}