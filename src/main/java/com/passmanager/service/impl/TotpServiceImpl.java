package com.passmanager.service.impl;

import com.passmanager.model.entity.User;
import com.passmanager.repository.UserRepository;
import com.passmanager.service.EncryptionService;
import com.passmanager.service.TotpService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TotpServiceImpl implements TotpService {

    private static final String APP_NAME = "KeyGuard";

    private final GoogleAuthenticator googleAuthenticator;
    private final EncryptionService encryptionService;
    private final UserRepository userRepository;

    public TotpServiceImpl(EncryptionService encryptionService, UserRepository userRepository) {
        this.googleAuthenticator = new GoogleAuthenticator();
        this.encryptionService = encryptionService;
        this.userRepository = userRepository;
    }

    @Override
    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    @Override
    public String generateQRCodeUrl(User user, String secret) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                APP_NAME,
                user.getUsername(),
                new GoogleAuthenticatorKey.Builder(secret).build()
        );
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        try {
            int codeInt = Integer.parseInt(code);
            return googleAuthenticator.authorize(secret, codeInt);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void enableTwoFactor(User user, String secret) {
        // Cifrar el secreto TOTP antes de guardarlo
        String encryptedSecret = encryptionService.encrypt(secret);
        user.setTotpSecret(encryptedSecret);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void disableTwoFactor(User user) {
        user.setTotpSecret(null);
        userRepository.save(user);
    }

    @Override
    public boolean isTwoFactorEnabled(User user) {
        return user.getTotpSecret() != null && !user.getTotpSecret().isEmpty();
    }
}
