package com.dmg.movieticket.service.impl;

import com.dmg.movieticket.entity.RefreshToken;
import com.dmg.movieticket.entity.User;
import com.dmg.movieticket.exception.ApplicationException;
import com.dmg.movieticket.exception.ErrorCode;
import com.dmg.movieticket.repository.RefreshTokenRepository;
import com.dmg.movieticket.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl
        implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.refresh-token.expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {

        /*
         * Keep only one refresh token per user.
         * Logging in again invalidates the previous refresh token.
         */
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiresAt(
                                LocalDateTime.now().plus(
                                        java.time.Duration.ofMillis(
                                                refreshTokenExpirationMs
                                        )
                                )
                        )
                        .revoked(false)
                        .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new ApplicationException(
                                        ErrorCode.UNAUTHORIZED,
                                        "Invalid refresh token"
                                )
                        );

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {

            throw new ApplicationException(
                    ErrorCode.UNAUTHORIZED,
                    "Refresh token has been revoked"
            );
        }

        if (!refreshToken.getExpiresAt()
                .isAfter(LocalDateTime.now())) {

            throw new ApplicationException(
                    ErrorCode.UNAUTHORIZED,
                    "Refresh token has expired"
            );
        }

        return refreshToken;
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new ApplicationException(
                                        ErrorCode.UNAUTHORIZED,
                                        "Invalid refresh token"
                                )
                        );

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
    }
}