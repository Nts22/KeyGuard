package com.passmanager.service.impl;

import com.passmanager.exception.EncryptionException;
import com.passmanager.service.EncryptionService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
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
import java.util.Arrays;
import java.util.Base64;

/**
 * Jerarquía de claves triple derivada de PBKDF2 (768 bits):
 *
 *   Key A [0:32]  — Autenticación: hash almacenado = SHA-256(Key A).
 *   Key B [32:64] — Cifrado AES-256-GCM de contraseñas.
 *   Key C [64:96] — Firma HMAC-SHA256 de integridad por entrada.
 *
 * Propiedad de PBKDF2: los primeros 256 bits de una salida de 768 bits son
 * idénticos a una salida de 256 bits con los mismos parámetros, por lo que
 * Key A es compatible con los hashes legacy (v1).
 */
@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final String AES_GCM        = "AES/GCM/NoPadding";
    private static final int    TOTAL_KEY_BITS = 768;   // 3 × 256
    private static final int    KEY_BYTES      = 32;    // 256 bits
    private static final int    IV_LENGTH      = 12;
    private static final int    GCM_TAG_BITS   = 128;
    private static final int    ITERATIONS     = 100_000;
    private static final int    SALT_LENGTH    = 16;

    private SecretKey keyA;
    private SecretKey keyB;
    private SecretKey keyC;

    // ---------------------------------------------------------------
    // Salt
    // ---------------------------------------------------------------

    @Override
    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // ---------------------------------------------------------------
    // Derivación y autenticación
    // ---------------------------------------------------------------

    @Override
    public void deriveKey(String masterPassword, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), saltBytes, ITERATIONS, TOTAL_KEY_BITS);
            byte[] raw = factory.generateSecret(spec).getEncoded();

            this.keyA = new SecretKeySpec(Arrays.copyOfRange(raw, 0,            KEY_BYTES), "AES");
            this.keyB = new SecretKeySpec(Arrays.copyOfRange(raw, KEY_BYTES,    2 * KEY_BYTES), "AES");
            this.keyC = new SecretKeySpec(Arrays.copyOfRange(raw, 2 * KEY_BYTES, 3 * KEY_BYTES), "AES");
        } catch (Exception e) {
            throw new EncryptionException("Error al derivar las claves", e);
        }
    }

    @Override
    public String hashPassword(String password, String salt) {
        byte[] keyABytes = deriveKeyABytes(password, salt);
        return Base64.getEncoder().encodeToString(sha256(keyABytes));
    }

    @Override
    public boolean verifyPassword(String password, String salt, String storedHash) {
        String computed = hashPassword(password, salt);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public boolean verifyPasswordLegacy(String password, String salt, String storedHash) {
        // v1: el hash almacenado es Base64(Key A) directamente, sin SHA-256
        byte[] keyABytes = deriveKeyABytes(password, salt);
        String computed = Base64.getEncoder().encodeToString(keyABytes);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void clearKey() {
        this.keyA = null;
        this.keyB = null;
        this.keyC = null;
    }

    @Override
    public boolean isKeyDerived() {
        return this.keyB != null;
    }

    // ---------------------------------------------------------------
    // Cifrado / Descifrado
    // ---------------------------------------------------------------

    @Override
    public String encrypt(String plainText) {
        requireKey(keyB, "Key B");
        return encryptWithKey(plainText, keyB);
    }

    @Override
    public String decrypt(String encryptedText) {
        requireKey(keyB, "Key B");
        return decryptWithKey(encryptedText, keyB);
    }

    @Override
    public String decryptLegacy(String encryptedText) {
        requireKey(keyA, "Key A");
        return decryptWithKey(encryptedText, keyA);
    }

    @Override
    public String migrateEncrypted(String legacyCiphertext) {
        return encrypt(decryptLegacy(legacyCiphertext));
    }

    // ---------------------------------------------------------------
    // Firma de integridad (HMAC-SHA256 con Key C)
    // ---------------------------------------------------------------

    @Override
    public String sign(String data) {
        requireKey(keyC, "Key C");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyC);
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new EncryptionException("Error al firmar datos", e);
        }
    }

    @Override
    public boolean verifySignature(String data, String signature) {
        requireKey(keyC, "Key C");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyC);
            byte[] expected = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            byte[] actual   = Base64.getDecoder().decode(signature);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            throw new EncryptionException("Error al verificar firma", e);
        }
    }

    // ---------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------

    /**
     * Deriva solo los primeros 256 bits (bloque 1 de PBKDF2) = Key A.
     * Estos bytes son idénticos a los primeros 32 bytes de la derivación
     * de 768 bits, permitiendo compatibilidad con hashes legacy.
     */
    private static byte[] deriveKeyABytes(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, 256);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new EncryptionException("Error al derivar Key A", e);
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new EncryptionException("Error SHA-256", e);
        }
    }

    private static void requireKey(SecretKey key, String name) {
        if (key == null) {
            throw new EncryptionException(name + " no ha sido derivada. Inicie sesión primero.");
        }
    }

    private static String encryptWithKey(String plainText, SecretKey key) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buf = ByteBuffer.allocate(IV_LENGTH + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new EncryptionException("Error al encriptar", e);
        }
    }

    private static String decryptWithKey(String encryptedText, SecretKey key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            ByteBuffer buf = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH];
            buf.get(iv);
            byte[] cipherText = new byte[buf.remaining()];
            buf.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Error al desencriptar", e);
        }
    }
}
