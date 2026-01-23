package com.passmanager.service.impl;

import com.passmanager.exception.EncryptionException;
import com.passmanager.service.RecoveryKeyService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
public class RecoveryKeyServiceImpl implements RecoveryKeyService {

    private static final int RECOVERY_KEY_LENGTH = 24; // 24 caracteres (6 grupos de 4)
    private static final String RECOVERY_KEY_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Sin I, O, 0, 1 para evitar confusión
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int KEY_LENGTH = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Override
    public String generateRecoveryKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder recoveryKey = new StringBuilder();

        for (int i = 0; i < RECOVERY_KEY_LENGTH; i++) {
            // Agregar guión cada 4 caracteres
            if (i > 0 && i % 4 == 0) {
                recoveryKey.append('-');
            }
            int index = random.nextInt(RECOVERY_KEY_CHARS.length());
            recoveryKey.append(RECOVERY_KEY_CHARS.charAt(index));
        }

        return recoveryKey.toString();
    }

    @Override
    public String hashRecoveryKey(String recoveryKey) {
        try {
            // Usar un salt fijo para recovery keys (ya que la recovery key en sí es aleatoria)
            byte[] salt = "RECOVERY_KEY_SALT_V1".getBytes(StandardCharsets.UTF_8);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                recoveryKey.replace("-", "").toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                KEY_LENGTH
            );
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new EncryptionException("Error al hashear recovery key", e);
        }
    }

    @Override
    public boolean verifyRecoveryKey(String recoveryKey, String storedHash) {
        String computedHash = hashRecoveryKey(recoveryKey);
        return MessageDigest.isEqual(
            computedHash.getBytes(StandardCharsets.UTF_8),
            storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public String encryptMasterPassword(String masterPassword, String recoveryKey) {
        try {
            // Derivar clave AES de la recovery key
            SecretKey key = deriveKeyFromRecoveryKey(recoveryKey);

            // Generar IV aleatorio
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Configurar cifrado AES-GCM
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Cifrar
            byte[] ciphertext = cipher.doFinal(masterPassword.getBytes(StandardCharsets.UTF_8));

            // Combinar IV + ciphertext
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            throw new EncryptionException("Error al cifrar contraseña maestra", e);
        }
    }

    @Override
    public String decryptMasterPassword(String encryptedMasterPassword, String recoveryKey) {
        try {
            // Derivar clave AES de la recovery key
            SecretKey key = deriveKeyFromRecoveryKey(recoveryKey);

            // Decodificar datos cifrados
            byte[] encryptedData = Base64.getDecoder().decode(encryptedMasterPassword);

            // Extraer IV y ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);
            System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            // Configurar descifrado AES-GCM
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // Descifrar
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Error al descifrar contraseña maestra. Recovery key inválida.", e);
        }
    }

    /**
     * Deriva una clave AES-256 de la recovery key usando PBKDF2.
     */
    private SecretKey deriveKeyFromRecoveryKey(String recoveryKey) {
        try {
            // Usar un salt fijo específico para derivación de clave
            byte[] salt = "RECOVERY_KEY_ENCRYPTION_SALT_V1".getBytes(StandardCharsets.UTF_8);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                recoveryKey.replace("-", "").toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                KEY_LENGTH
            );
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new EncryptionException("Error al derivar clave de recovery key", e);
        }
    }
}
