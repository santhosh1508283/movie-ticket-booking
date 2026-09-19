package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.*;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.repository.UserRepository;
import com.dmg.movieticket.security.JwtService;
import com.dmg.movieticket.service.RefreshTokenService;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {
    UserRepository users = mock(UserRepository.class);
    PasswordEncoder passwords = mock(PasswordEncoder.class);
    AuthenticationManager authentication = mock(AuthenticationManager.class);
    JwtService jwt = mock(JwtService.class);
    RefreshTokenService refresh = mock(RefreshTokenService.class);
    AuthServiceImpl service = new AuthServiceImpl(users, passwords, authentication, jwt, refresh);
    void tokens() {
        when(jwt.generateToken("customer@example.test")).thenReturn("access");
        when(refresh.createRefreshToken(any())).thenAnswer(i -> RefreshToken.builder().user(i.getArgument(0)).token("refresh").build());
    }
    @Test void registerNormalizesIdentityHashesPasswordAndAssignsCustomerRole() {
        tokens(); when(passwords.encode("password123")).thenReturn("hash");
        when(users.save(any())).thenAnswer(i -> {
            User user = i.getArgument(0); assertEquals("hash", user.getPassword()); user.setId(1L); return user;
        });
        var result = service.register(new RegisterRequest(" Customer ", " CUSTOMER@example.test ", "password123"));
        assertEquals("Customer", result.name()); assertEquals("customer@example.test", result.email());
        assertEquals(Role.CUSTOMER, result.role()); assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken()); verify(users).existsByEmail("customer@example.test");
    }
    @Test void rejectsDuplicateRegistrationWithoutHashingOrIssuingTokens() {
        when(users.existsByEmail("customer@example.test")).thenReturn(true);
        error(DUPLICATE_RESOURCE, () -> service.register(new RegisterRequest("Customer", "CUSTOMER@example.test", "password123")));
        verifyNoInteractions(passwords, jwt, refresh); verify(users, never()).save(any());
    }
    @Test void loginAuthenticatesNormalizedEmail() {
        tokens(); when(users.findByEmail("customer@example.test")).thenReturn(Optional.of(user()));
        assertEquals("access", service.login(new LoginRequest(" CUSTOMER@example.test ", "password123")).accessToken());
        verify(authentication).authenticate(argThat(a -> a.getName().equals("customer@example.test")
                && a.getCredentials().equals("password123")));
    }
    @Test void badCredentialsReturnUnauthorizedWithoutTokens() {
        when(authentication.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        error(UNAUTHORIZED, () -> service.login(new LoginRequest("customer@example.test", "bad")));
        verifyNoInteractions(jwt, refresh);
    }
    @Test void missingUserAfterAuthenticationReturnsUnauthorized() {
        error(UNAUTHORIZED, () -> service.login(new LoginRequest("missing@example.test", "password123")));
        verifyNoInteractions(jwt, refresh);
    }
    @Test void refreshKeepsExistingRefreshToken() {
        when(refresh.verifyRefreshToken("refresh")).thenReturn(RefreshToken.builder().user(user()).token("refresh").build());
        when(jwt.generateToken("customer@example.test")).thenReturn("new-access");
        var result = service.refresh(new RefreshTokenRequest(" refresh "));
        assertEquals("new-access", result.accessToken()); assertEquals("refresh", result.refreshToken());
        verify(refresh, never()).createRefreshToken(any());
    }
    @Test void logoutRevokesTrimmedToken() {
        service.logout(new RefreshTokenRequest(" refresh ")); verify(refresh).revokeRefreshToken("refresh");
    }
}
