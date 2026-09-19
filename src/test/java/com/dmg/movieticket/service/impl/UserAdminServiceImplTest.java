package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.UpdateUserRoleRequest;
import com.dmg.movieticket.entity.*;
import com.dmg.movieticket.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static com.dmg.movieticket.support.TestFixtures.*;
import static com.dmg.movieticket.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserAdminServiceImplTest {
    UserRepository repository = mock(UserRepository.class);
    UserAdminServiceImpl service = new UserAdminServiceImpl(repository);
    @Test void promotesNormalizedEmailToAdmin() {
        User user = user(); when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);
        var result = service.makeAdmin(new UpdateUserRoleRequest(" CUSTOMER@example.test "));
        assertEquals(Role.ADMIN, result.role()); assertEquals(1L, result.userId()); verify(repository).save(user);
    }
    @Test void rejectsMissingUser() {
        error(RESOURCE_NOT_FOUND, () -> service.makeAdmin(new UpdateUserRoleRequest("missing@example.test")));
        verify(repository, never()).save(any());
    }
    @Test void rejectsExistingAdmin() {
        User user = user(); user.setRole(Role.ADMIN); when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        error(INVALID_STATE, () -> service.makeAdmin(new UpdateUserRoleRequest(user.getEmail())));
        verify(repository, never()).save(any());
    }
}
