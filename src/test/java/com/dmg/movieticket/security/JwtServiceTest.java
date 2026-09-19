package com.dmg.movieticket.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    JwtService service;
    @BeforeEach void setUp() {
        service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", Base64.getEncoder().encodeToString(
                "test-only-signing-key-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(service, "expirationMs", 60000L);
    }
    @Test void generatedTokenContainsEmailAndValidSignature() {
        String token = service.generateToken("customer@example.test");
        assertEquals("customer@example.test", service.extractEmail(token));
        assertTrue(service.isTokenValid(token, "customer@example.test"));
        assertFalse(service.isTokenValid(token, "another@example.test"));
    }
    @Test void rejectsExpiredTokenWithoutSleeping() {
        ReflectionTestUtils.setField(service, "expirationMs", -60000L);
        String token = service.generateToken("customer@example.test");
        assertThrows(JwtException.class, () -> service.isTokenValid(token, "customer@example.test"));
    }
    @Test void rejectsSignatureFromAnotherKey() {
        String token = service.generateToken("customer@example.test");
        ReflectionTestUtils.setField(service, "secret", Base64.getEncoder().encodeToString(
                "different-test-signing-key-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)));
        assertThrows(JwtException.class, () -> service.extractEmail(token));
    }
    @Test void rejectsMalformedToken() { assertThrows(JwtException.class, () -> service.extractEmail("not.a.token")); }
}
