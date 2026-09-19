package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.LoginRequest;
import com.dmg.movieticket.dto.request.RefreshTokenRequest;
import com.dmg.movieticket.dto.request.RegisterRequest;
import com.dmg.movieticket.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(
            RegisterRequest request
    );

    AuthResponse login(
            LoginRequest request
    );

    AuthResponse refresh(
            RefreshTokenRequest request
    );

    void logout(
            RefreshTokenRequest request
    );
}