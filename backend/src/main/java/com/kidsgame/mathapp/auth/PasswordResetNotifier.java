package com.kidsgame.mathapp.auth;

public interface PasswordResetNotifier {
    void send(String email, String resetUrl);
}
