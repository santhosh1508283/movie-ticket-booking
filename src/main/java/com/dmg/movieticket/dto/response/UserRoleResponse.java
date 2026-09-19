package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.Role;

public record UserRoleResponse(
        Long userId,
        String name,
        String email,
        Role role
) {
}