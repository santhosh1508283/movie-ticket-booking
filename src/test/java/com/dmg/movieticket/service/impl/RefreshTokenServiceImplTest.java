package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.repository.RefreshTokenRepository;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceImplTest {
    RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    RefreshTokenServiceImpl service = new RefreshTokenServiceImpl(repository);
    @BeforeEach void setUp() { ReflectionTestUtils.setField(service, "refreshTokenExpirationMs", 60000L); }
    RefreshToken validToken() {
        return RefreshToken.builder().token("token").user(user()).revoked(false).expiresAt(LocalDateTime.now().plusHours(1)).build();
    }
    @Test void createsUniqueExpiringTokenForUserWithoutToken() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        LocalDateTime before = LocalDateTime.now();
        var result = service.createRefreshToken(user());
        assertNotNull(UUID.fromString(result.getToken())); assertFalse(result.getRevoked());
        assertFalse(result.getExpiresAt().isBefore(before.plusMinutes(1)));
        assertFalse(result.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(1)));
        verify(repository).findByUserId(1L); verify(repository).save(result);
    }
    @Test void rotatesExistingTokenInPlaceAndClearsRevocation() {
        var existing = validToken(); existing.setId(8L); existing.setRevoked(true);
        existing.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(repository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.createRefreshToken(user());
        assertSame(existing, result); assertEquals(8L, result.getId());
        assertNotEquals("token", result.getToken()); assertFalse(result.getRevoked());
        assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now()));
        verify(repository, never()).deleteByUserId(anyLong());
    }
    @Test void verifiesValidToken() {
        var token = validToken(); when(repository.findByToken("token")).thenReturn(Optional.of(token));
        assertSame(token, service.verifyRefreshToken("token"));
    }
    @Test void rejectsUnknownToken() { error(UNAUTHORIZED, () -> service.verifyRefreshToken("unknown")); }
    @Test void rejectsRevokedToken() {
        var token = validToken(); token.setRevoked(true); when(repository.findByToken("token")).thenReturn(Optional.of(token));
        error(UNAUTHORIZED, () -> service.verifyRefreshToken("token"));
    }
    @Test void rejectsExpiredToken() {
        var token = validToken(); token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(repository.findByToken("token")).thenReturn(Optional.of(token));
        error(UNAUTHORIZED, () -> service.verifyRefreshToken("token"));
    }
    @Test void revokesToken() {
        var token = validToken(); when(repository.findByToken("token")).thenReturn(Optional.of(token));
        service.revokeRefreshToken("token"); assertTrue(token.getRevoked()); verify(repository).save(token);
    }
    @Test void rejectsRevokingUnknownToken() {
        error(UNAUTHORIZED, () -> service.revokeRefreshToken("unknown")); verify(repository, never()).save(any());
    }
}
