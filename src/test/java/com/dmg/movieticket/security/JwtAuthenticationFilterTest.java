package com.dmg.movieticket.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    JwtService jwt = mock(JwtService.class);
    CustomUserDetailsService users = mock(CustomUserDetailsService.class);
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users);
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void authenticatesValidBearerToken() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        var user = User.withUsername("customer@example.test").password("hash").roles("CUSTOMER").build();
        when(jwt.extractEmail("token")).thenReturn(user.getUsername());
        when(users.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwt.isTokenValid("token", user.getUsername())).thenReturn(true);
        filter.doFilter(request, response, chain);
        assertEquals(user.getUsername(), SecurityContextHolder.getContext().getAuthentication().getName());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        verify(chain).doFilter(request, response);
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"Basic abc", "bearer token"})
    void skipsMissingOrNonBearerHeaders(String header) throws Exception {
        if (header != null) request.addHeader("Authorization", header);
        filter.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication()); verifyNoInteractions(jwt, users);
        verify(chain).doFilter(request, response);
    }
    @Test void malformedTokenLeavesRequestUnauthenticatedAndContinues() throws Exception {
        request.addHeader("Authorization", "Bearer broken");
        when(jwt.extractEmail("broken")).thenThrow(new IllegalArgumentException("bad token"));
        filter.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication()); verify(chain).doFilter(request, response);
    }
    @Test void invalidTokenDoesNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer token"); when(jwt.extractEmail("token")).thenReturn("customer@example.test");
        when(users.loadUserByUsername("customer@example.test")).thenReturn(User.withUsername("customer@example.test").password("hash").roles("CUSTOMER").build());
        filter.doFilter(request, response, chain); assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void preservesExistingAuthentication() throws Exception {
        var existing = new UsernamePasswordAuthenticationToken("existing", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer token"); when(jwt.extractEmail("token")).thenReturn("another");
        filter.doFilter(request, response, chain);
        assertSame(existing, SecurityContextHolder.getContext().getAuthentication()); verifyNoInteractions(users);
    }
}
