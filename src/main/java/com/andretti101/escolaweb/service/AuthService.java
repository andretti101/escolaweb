package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.ChangePasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.LoginRequestDTO;
import com.andretti101.escolaweb.dto.request.RefreshTokenRequestDTO;
import com.andretti101.escolaweb.dto.response.AuthResponseDTO;

public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO dto);
    AuthResponseDTO refresh(RefreshTokenRequestDTO dto);
    void logout(RefreshTokenRequestDTO dto);
    void changePassword(String email, ChangePasswordRequestDTO dto);
}
