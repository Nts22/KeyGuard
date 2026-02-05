package com.passmanager.service;

/**
 * Servicio de cifrado con jerarquía de claves triple.
 *
 * Todas las claves se derivan de la contraseña maestra mediante una única
 * llamada a PBKDF2 con salida de 768 bits, dividida en tres bloques:
 *
 *   Key A [bytes 0-31]  — Autenticación de bóveda (hash almacenado = SHA-256(Key A))
 *   Key B [bytes 32-63] — Cifrado AES-256-GCM de contraseñas
 *   Key C [bytes 64-95] — Firma HMAC-SHA256 de integridad
 */
public interface EncryptionService {

    String generateSalt();

    /** Retorna el hash v2 para almacenamiento: Base64(SHA-256(Key A)). */
    String hashPassword(String password, String salt);

    /** Verifica contra un hash v2 (SHA-256 de Key A). */
    boolean verifyPassword(String password, String salt, String storedHash);

    /** Verifica contra un hash v1 legacy (PBKDF2 raw 256-bit = Key A sin SHA-256). */
    boolean verifyPasswordLegacy(String password, String salt, String storedHash);

    /** Deriva las tres claves de sesión (Key A, Key B, Key C). */
    void deriveKey(String masterPassword, String salt);

    void clearKey();

    boolean isKeyDerived();

    /** Cifra con Key B (AES-256-GCM). */
    String encrypt(String plainText);

    /** Descifra con Key B (AES-256-GCM). */
    String decrypt(String encryptedText);

    /** Descifra con Key A — para datos cifrados con el esquema legacy (v1). */
    String decryptLegacy(String encryptedText);

    /** Re-cifra de Key A a Key B en una sola operación (migración v1 → v2). */
    String migrateEncrypted(String legacyCiphertext);

    /** Firma datos con HMAC-SHA256 usando Key C. Retorna Base64. */
    String sign(String data);

    /** Verifica una firma HMAC-SHA256 generada con Key C. */
    boolean verifySignature(String data, String signature);
}
