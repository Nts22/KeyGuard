package com.passmanager.service.impl;

import com.passmanager.service.PasswordGeneratorService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class PasswordGeneratorServiceImpl implements PasswordGeneratorService {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate(int length, boolean includeUppercase, boolean includeDigits, boolean includeSymbols) {
        if (length < 4) {
            length = 4;
        }

        StringBuilder charPool = new StringBuilder(LOWERCASE);
        StringBuilder password = new StringBuilder();

        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));

        if (includeUppercase) {
            charPool.append(UPPERCASE);
            password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        }

        if (includeDigits) {
            charPool.append(DIGITS);
            password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }

        if (includeSymbols) {
            charPool.append(SYMBOLS);
            password.append(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
        }

        String pool = charPool.toString();
        while (password.length() < length) {
            password.append(pool.charAt(random.nextInt(pool.length())));
        }

        return shuffle(password.toString());
    }

    @Override
    public String generateDefault() {
        return generate(16, true, true, true);
    }

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

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
