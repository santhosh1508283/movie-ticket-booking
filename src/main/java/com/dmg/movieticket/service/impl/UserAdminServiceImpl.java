package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.UpdateUserRoleRequest;
import com.dmg.movieticket.dto.response.UserRoleResponse;
import com.dmg.movieticket.entity.Role;
import com.dmg.movieticket.entity.User;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.repository.UserRepository;
import com.dmg.movieticket.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserRoleResponse makeAdmin(
            UpdateUserRoleRequest request
    ) {

        String email = request.email()
                .trim()
                .toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ApplicationException(
                                ErrorCode.RESOURCE_NOT_FOUND,
                                "User not found with email: " + email
                        )
                );

        if (user.getRole() == Role.ADMIN) {
            throw new ApplicationException(
                    ErrorCode.INVALID_STATE,
                    "User is already an admin"
            );
        }

        user.setRole(Role.ADMIN);

        User savedUser =
                userRepository.save(user);

        return new UserRoleResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }
}