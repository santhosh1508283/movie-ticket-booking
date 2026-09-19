package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.LoginRequest;
import com.dmg.movieticket.dto.request.RefreshTokenRequest;
import com.dmg.movieticket.dto.request.RegisterRequest;
import com.dmg.movieticket.dto.response.AuthResponse;
import com.dmg.movieticket.entity.RefreshToken;
import com.dmg.movieticket.entity.Role;
import com.dmg.movieticket.entity.User;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.repository.UserRepository;
import com.dmg.movieticket.security.JwtService;
import com.dmg.movieticket.service.AuthService;
import com.dmg.movieticket.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl
        implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResponse register(
            RegisterRequest request
    ) {

        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        if (userRepository.existsByEmail(email)) {

            throw new ApplicationException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "User already exists with email: " + email
            );
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .role(Role.CUSTOMER)
                .build();

        User savedUser =
                userRepository.save(user);

        String accessToken =
                jwtService.generateToken(
                        savedUser.getEmail()
                );

        RefreshToken refreshToken =
                refreshTokenService
                        .createRefreshToken(savedUser);

        return buildAuthResponse(
                savedUser,
                accessToken,
                refreshToken.getToken()
        );
    }

    @Override
    @Transactional
    public AuthResponse login(
            LoginRequest request
    ) {

        String email =
                request.email()
                        .trim()
                        .toLowerCase();

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.password()
                    )
            );

        } catch (AuthenticationException exception) {

            throw new ApplicationException(
                    ErrorCode.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ApplicationException(
                                        ErrorCode.UNAUTHORIZED,
                                        "Invalid email or password"
                                )
                        );

        String accessToken =
                jwtService.generateToken(
                        user.getEmail()
                );

        RefreshToken refreshToken =
                refreshTokenService
                        .createRefreshToken(user);

        return buildAuthResponse(
                user,
                accessToken,
                refreshToken.getToken()
        );
    }

    @Override
    @Transactional
    public AuthResponse refresh(
            RefreshTokenRequest request
    ) {

        RefreshToken refreshToken =
                refreshTokenService
                        .verifyRefreshToken(
                                request.refreshToken().trim()
                        );

        User user =
                refreshToken.getUser();

        String newAccessToken =
                jwtService.generateToken(
                        user.getEmail()
                );

        /*
         * Keep the same refresh token until it expires/logout.
         */
        return buildAuthResponse(
                user,
                newAccessToken,
                refreshToken.getToken()
        );
    }

    @Override
    @Transactional
    public void logout(
            RefreshTokenRequest request
    ) {

        refreshTokenService.revokeRefreshToken(
                request.refreshToken().trim()
        );
    }

    private AuthResponse buildAuthResponse(
            User user,
            String accessToken,
            String refreshToken
    ) {

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                refreshToken
        );
    }
}