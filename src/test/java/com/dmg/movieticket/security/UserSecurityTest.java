package com.dmg.movieticket.security;

import com.dmg.movieticket.entity.Role;
import com.dmg.movieticket.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.*;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserSecurityTest {
    UserRepository repository = mock(UserRepository.class);
    CurrentUserService current = new CurrentUserService(repository);
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void resolvesAuthenticatedUserAndId() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("customer@example.test", null, List.of()));
        when(repository.findByEmail("customer@example.test")).thenReturn(Optional.of(user()));
        assertEquals(1L, current.getCurrentUserId());
    }
    @Test void rejectsMissingAuthentication() {
        SecurityContextHolder.clearContext(); error(INVALID_REQUEST, current::getCurrentUser); verifyNoInteractions(repository);
    }
    @Test void rejectsUnauthenticatedToken() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("customer@example.test", "password"));
        error(INVALID_REQUEST, current::getCurrentUser); verifyNoInteractions(repository);
    }
    @Test void rejectsDeletedAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("missing@example.test", null, List.of()));
        error(RESOURCE_NOT_FOUND, current::getCurrentUser);
    }
    @ParameterizedTest @EnumSource(Role.class)
    void userDetailsExposesStoredPasswordAndRole(Role role) {
        var user = user(); user.setRole(role);
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var details = new CustomUserDetailsService(repository).loadUserByUsername(user.getEmail());
        assertEquals(user.getPassword(), details.getPassword());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role)));
    }
    @Test void userDetailsRejectsUnknownUser() {
        assertThrows(UsernameNotFoundException.class, () -> new CustomUserDetailsService(repository).loadUserByUsername("missing"));
    }
}
