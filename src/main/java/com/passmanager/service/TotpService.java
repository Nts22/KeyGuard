package com.passmanager.service;

import com.passmanager.model.entity.User;

public interface TotpService {

    /**
     * Genera un nuevo secreto TOTP (Base32) para un usuario.
     * @return Secreto TOTP en formato Base32
     */
    String generateSecret();

    /**
     * Genera la URL para mostrar el código QR de configuración 2FA.
     * Compatible con Google Authenticator, Authy, etc.
     * @param user Usuario para el que se genera el QR
     * @param secret Secreto TOTP
     * @return URL en formato otpauth://totp/...
     */
    String generateQRCodeUrl(User user, String secret);

    /**
     * Verifica si un código TOTP es válido para el secreto del usuario.
     * @param secret Secreto TOTP del usuario
     * @param code Código de 6 dígitos ingresado
     * @return true si el código es válido
     */
    boolean verifyCode(String secret, String code);

    /**
     * Habilita 2FA para un usuario, guardando el secreto cifrado.
     * @param user Usuario para habilitar 2FA
     * @param secret Secreto TOTP generado
     */
    void enableTwoFactor(User user, String secret);

    /**
     * Deshabilita 2FA para un usuario.
     * @param user Usuario para deshabilitar 2FA
     */
    void disableTwoFactor(User user);

    /**
     * Verifica si un usuario tiene 2FA habilitado.
     * @param user Usuario a verificar
     * @return true si 2FA está habilitado
     */
    boolean isTwoFactorEnabled(User user);
}
