package com.dmg.movieticket.dto.response;

import com.dmg.movieticket.entity.Role;

public record AuthResponse(
        Long userId,
        String name,
        String email,
        Role role,
        String accessToken
) {
}