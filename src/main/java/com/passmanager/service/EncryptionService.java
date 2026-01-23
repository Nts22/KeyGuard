package com.passmanager.service;

public interface EncryptionService {

    String generateSalt();

    String hashPassword(String password, String salt);

    boolean verifyPassword(String password, String salt, String storedHash);

    void deriveKey(String masterPassword, String salt);

    void clearKey();

    boolean isKeyDerived();

    String encrypt(String plainText);

    String decrypt(String encryptedText);
}
