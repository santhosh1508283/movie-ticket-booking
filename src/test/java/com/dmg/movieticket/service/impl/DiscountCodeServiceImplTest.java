package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.CreateDiscountCodeRequest;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.mapper.DiscountCodeMapper;
import com.dmg.movieticket.repository.DiscountCodeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountCodeServiceImplTest {
    DiscountCodeRepository repository = mock(DiscountCodeRepository.class);
    DiscountCodeServiceImpl service = new DiscountCodeServiceImpl(repository, Mappers.getMapper(DiscountCodeMapper.class));
    LocalDateTime start = LocalDateTime.of(2030, 1, 1, 0, 0);
    CreateDiscountCodeRequest request(DiscountType type, String amount, LocalDateTime end) {
        return new CreateDiscountCodeRequest(" save ", type, money(amount), money("50"), money("30"), start, end);
    }
    @Test void createsNormalizedActiveCode() {
        when(repository.save(any())).thenAnswer(i -> { DiscountCode d = i.getArgument(0); d.setId(1L); return d; });
        var result = service.createDiscountCode(request(DiscountType.PERCENTAGE, "20", start.plusDays(1)));
        assertEquals("SAVE", result.code()); assertTrue(result.active()); assertEquals(money("20"), result.discountValue());
        assertEquals(money("30"), result.maximumDiscountAmount()); verify(repository).existsByCodeIgnoreCase("SAVE");
    }
    @Test void rejectsDuplicateCode() {
        when(repository.existsByCodeIgnoreCase("SAVE")).thenReturn(true);
        error(DUPLICATE_RESOURCE, () -> service.createDiscountCode(request(DiscountType.FLAT, "20", start.plusDays(1))));
        verify(repository, never()).save(any());
    }
    @ParameterizedTest @ValueSource(ints = {0, -1})
    void rejectsInvalidValidityRange(int days) {
        error(INVALID_REQUEST, () -> service.createDiscountCode(request(DiscountType.FLAT, "20", start.plusDays(days))));
        verify(repository, never()).save(any());
    }
    @Test void rejectsPercentageOver100() {
        error(INVALID_REQUEST, () -> service.createDiscountCode(request(DiscountType.PERCENTAGE, "100.01", start.plusDays(1))));
    }
    @Test void acceptsExactly100PercentAndFlatValueOver100() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(money("100"), service.createDiscountCode(request(DiscountType.PERCENTAGE, "100", start.plusDays(1))).discountValue());
        assertEquals(money("150"), service.createDiscountCode(request(DiscountType.FLAT, "150", start.plusDays(1))).discountValue());
    }
    @Test void listsCodesAndEmptyResults() {
        when(repository.findAll()).thenReturn(List.of(DiscountCode.builder().id(1L).code("SAVE").build())).thenReturn(List.of());
        assertEquals("SAVE", service.getAllDiscountCodes().get(0).code()); assertTrue(service.getAllDiscountCodes().isEmpty());
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void changesActiveFlag(boolean active) {
        DiscountCode code = DiscountCode.builder().id(1L).active(!active).build();
        when(repository.findById(1L)).thenReturn(Optional.of(code)); when(repository.save(code)).thenReturn(code);
        assertEquals(active, service.setActive(1L, active).active()); verify(repository).save(code);
    }
    @Test void rejectsMissingCodeUpdate() { error(RESOURCE_NOT_FOUND, () -> service.setActive(99L, false)); }
}
