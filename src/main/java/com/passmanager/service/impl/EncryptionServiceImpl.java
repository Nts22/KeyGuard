package com.passmanager.service.impl;

import com.passmanager.exception.EncryptionException;
import com.passmanager.service.EncryptionService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int ITERATIONS = 100_000;
    private static final int SALT_LENGTH = 16;

    private SecretKey derivedKey;

    @Override
    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @Override
    public String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new EncryptionException("Error al hashear la contraseña", e);
        }
    }

    @Override
    public boolean verifyPassword(String password, String salt, String storedHash) {
        String computedHash = hashPassword(password, salt);
        return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void deriveKey(String masterPassword, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            this.derivedKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new EncryptionException("Error al derivar la clave", e);
        }
    }

    @Override
    public void clearKey() {
        this.derivedKey = null;
    }

    @Override
    public boolean isKeyDerived() {
        return this.derivedKey != null;
    }

    @Override
    public String encrypt(String plainText) {
        if (derivedKey == null) {
            throw new EncryptionException("La clave no ha sido derivada. Inicie sesión primero.");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, derivedKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(IV_LENGTH + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new EncryptionException("Error al encriptar", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        if (derivedKey == null) {
            throw new EncryptionException("La clave no ha sido derivada. Inicie sesión primero.");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, derivedKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Error al desencriptar", e);
        }
    }
}
