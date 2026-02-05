package com.passmanager.service.impl;

import com.passmanager.service.PasswordGeneratorService;
import org.springframework.stereotype.Service;

@Service
public class PasswordGeneratorServiceImpl implements PasswordGeneratorService {

    @Override
    public int calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.length() >= 16) score++;

        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")) score++;

        return Math.min(100, score * 15);
    }
}
