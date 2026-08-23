package com.andretti101.escolaweb.service;

public interface EmailService {

    void sendPasswordResetEmail(String to, String userName, String token);
}
