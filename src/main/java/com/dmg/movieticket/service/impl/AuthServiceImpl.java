package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.dto.request.LoginRequest;
import com.dmg.movieticket.dto.request.RegisterRequest;
import com.dmg.movieticket.dto.response.AuthResponse;
import com.dmg.movieticket.entity.Role;
import com.dmg.movieticket.entity.User;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.repository.UserRepository;
import com.dmg.movieticket.security.JwtService;
import com.dmg.movieticket.service.AuthService;
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
                    "User already exists with email: "
                            + email
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

        String token =
                jwtService.generateToken(
                        savedUser.getEmail()
                );

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                token
        );
    }

    @Override
    @Transactional(readOnly = true)
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

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new ApplicationException(
                                ErrorCode.UNAUTHORIZED,
                                "Invalid email or password"
                        )
                );

        String token =
                jwtService.generateToken(
                        user.getEmail()
                );

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }
}