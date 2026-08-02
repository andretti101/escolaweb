package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.RefreshToken;
import com.andretti101.escolaweb.model.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    RefreshToken rotate(String token);

    void revoke(String token);

    void revokeAllByUser(User user);
}
