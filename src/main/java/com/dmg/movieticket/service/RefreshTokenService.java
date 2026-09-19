package com.dmg.movieticket.service;

import com.dmg.movieticket.entity.RefreshToken;
import com.dmg.movieticket.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken verifyRefreshToken(String token);

    void revokeRefreshToken(String token);
}