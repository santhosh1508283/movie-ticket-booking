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

import java.time.Duration;
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

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByUserId(user.getId())
                        .orElseGet(() ->
                                RefreshToken.builder()
                                        .user(user)
                                        .build()
                        );

        refreshToken.setToken(
                UUID.randomUUID().toString()
        );

        refreshToken.setExpiresAt(
                LocalDateTime.now().plus(
                        Duration.ofMillis(
                                refreshTokenExpirationMs
                        )
                )
        );

        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(
                refreshToken
        );
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