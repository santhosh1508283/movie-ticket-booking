package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.CreateRefundPolicyRequest;
import com.dmg.movieticket.dto.response.RefundPolicyResponse;
import com.dmg.movieticket.service.RefundPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refund-policies")
@RequiredArgsConstructor
public class RefundPolicyController {

    private final RefundPolicyService refundPolicyService;

    @PostMapping
    public ResponseEntity<RefundPolicyResponse> create(
            @Valid @RequestBody CreateRefundPolicyRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        refundPolicyService
                                .createPolicy(request)
                );
    }

    @GetMapping
    public ResponseEntity<List<RefundPolicyResponse>> getAll() {

        return ResponseEntity.ok(
                refundPolicyService
                        .getAllPolicies()
        );
    }

    @PatchMapping("/{policyId}/active")
    public ResponseEntity<RefundPolicyResponse> setActive(
            @PathVariable Long policyId,
            @RequestParam boolean active
    ) {

        return ResponseEntity.ok(
                refundPolicyService.setActive(
                        policyId,
                        active
                )
        );
    }
}