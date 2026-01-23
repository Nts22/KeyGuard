package com.passmanager.service;

/**
 * Servicio para manejar claves de recuperación (Recovery Keys).
 * Las recovery keys permiten a los usuarios recuperar el acceso si olvidan su contraseña maestra.
 */
public interface RecoveryKeyService {

    /**
     * Genera una nueva recovery key en formato legible (ej: XKJF-9PQW-2M7N-VBRL-8HTG-4ZCS).
     *
     * @return Recovery key generada
     */
    String generateRecoveryKey();

    /**
     * Hashea una recovery key para almacenamiento seguro.
     *
     * @param recoveryKey La recovery key a hashear
     * @return Hash de la recovery key
     */
    String hashRecoveryKey(String recoveryKey);

    /**
     * Verifica si una recovery key coincide con el hash almacenado.
     *
     * @param recoveryKey Recovery key proporcionada por el usuario
     * @param storedHash Hash almacenado en la base de datos
     * @return true si la recovery key es válida
     */
    boolean verifyRecoveryKey(String recoveryKey, String storedHash);

    /**
     * Cifra la contraseña maestra usando la recovery key.
     *
     * @param masterPassword Contraseña maestra a cifrar
     * @param recoveryKey Recovery key para cifrar
     * @return Contraseña maestra cifrada en Base64
     */
    String encryptMasterPassword(String masterPassword, String recoveryKey);

    /**
     * Descifra la contraseña maestra usando la recovery key.
     *
     * @param encryptedMasterPassword Contraseña maestra cifrada
     * @param recoveryKey Recovery key para descifrar
     * @return Contraseña maestra original
     */
    String decryptMasterPassword(String encryptedMasterPassword, String recoveryKey);
}
