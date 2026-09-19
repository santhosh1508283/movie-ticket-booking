package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateRefundPolicyRequest;
import com.dmg.movieticket.entity.RefundPolicy;
import com.dmg.movieticket.mapper.RefundPolicyMapper;
import com.dmg.movieticket.repository.RefundPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundPolicyServiceImplTest {
    RefundPolicyRepository repository = mock(RefundPolicyRepository.class);
    RefundPolicyServiceImpl service = new RefundPolicyServiceImpl(repository, Mappers.getMapper(RefundPolicyMapper.class));
    @Test void createsActivePolicy() {
        when(repository.save(any())).thenAnswer(i -> { RefundPolicy p = i.getArgument(0); p.setId(1L); return p; });
        var result = service.createPolicy(new CreateRefundPolicyRequest(24, money("75")));
        assertEquals(24, result.hoursBeforeShow()); assertEquals(money("75"), result.refundPercentage());
        assertTrue(result.active()); verify(repository).existsByHoursBeforeShow(24);
    }
    @Test void rejectsDuplicateThreshold() {
        when(repository.existsByHoursBeforeShow(24)).thenReturn(true);
        error(DUPLICATE_RESOURCE, () -> service.createPolicy(new CreateRefundPolicyRequest(24, money("75"))));
        verify(repository, never()).save(any());
    }
    @Test void listsPoliciesAndEmptyResults() {
        when(repository.findAll()).thenReturn(List.of(RefundPolicy.builder().id(1L).hoursBeforeShow(24).build())).thenReturn(List.of());
        assertEquals(24, service.getAllPolicies().get(0).hoursBeforeShow()); assertTrue(service.getAllPolicies().isEmpty());
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void changesActiveFlag(boolean active) {
        var policy = RefundPolicy.builder().id(1L).active(!active).build();
        when(repository.findById(1L)).thenReturn(Optional.of(policy)); when(repository.save(policy)).thenReturn(policy);
        assertEquals(active, service.setActive(1L, active).active()); verify(repository).save(policy);
    }
    @Test void rejectsMissingPolicyUpdate() { error(RESOURCE_NOT_FOUND, () -> service.setActive(99L, false)); }
}
