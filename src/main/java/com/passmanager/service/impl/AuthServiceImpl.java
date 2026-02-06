package com.passmanager.service.impl;

import com.passmanager.exception.AuthenticationException;
import com.passmanager.model.dto.UserDTO;
import com.passmanager.model.entity.CustomField;
import com.passmanager.model.entity.PasswordEntry;
import com.passmanager.model.entity.User;
import com.passmanager.repository.PasswordEntryRepository;
import com.passmanager.repository.UserRepository;
import com.passmanager.service.AuthService;
import com.passmanager.service.EncryptionService;
import com.passmanager.service.LoginAttemptService;
import com.passmanager.service.RecoveryKeyService;
import com.passmanager.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEntryRepository passwordEntryRepository;
    private final UserService userService;
    private final EncryptionService encryptionService;
    private final LoginAttemptService loginAttemptService;
    private final RecoveryKeyService recoveryKeyService;
    private final com.passmanager.service.AuditLogService auditLogService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEntryRepository passwordEntryRepository,
                           UserService userService,
                           EncryptionService encryptionService,
                           LoginAttemptService loginAttemptService,
                           RecoveryKeyService recoveryKeyService,
                           com.passmanager.service.AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEntryRepository = passwordEntryRepository;
        this.userService = userService;
        this.encryptionService = encryptionService;
        this.loginAttemptService = loginAttemptService;
        this.recoveryKeyService = recoveryKeyService;
        this.auditLogService = auditLogService;
    }

    @Override
    public boolean hasUsers() {
        return userRepository.count() > 0;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userService.findAll();
    }

    @Override
    @Transactional
    public UserCreationResult createUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new AuthenticationException("El usuario ya existe: " + username);
        }

        if (password.length() < 8) {
            throw new AuthenticationException("La contraseña debe tener al menos 8 caracteres");
        }

        String salt = encryptionService.generateSalt();
        String hash = encryptionService.hashPassword(password, salt);

        // Generar recovery key
        String recoveryKey = recoveryKeyService.generateRecoveryKey();
        String recoveryKeyHash = recoveryKeyService.hashRecoveryKey(recoveryKey);
        String encryptedMasterPassword = recoveryKeyService.encryptMasterPassword(password, recoveryKey);

        User user = User.builder()
                .username(username)
                .passwordHash(hash)
                .salt(salt)
                .recoveryKeyHash(recoveryKeyHash)
                .encryptedMasterPassword(encryptedMasterPassword)
                .keyVersion(2)
                .build();

        user = userRepository.save(user);

        encryptionService.deriveKey(password, salt);
        userService.setCurrentUser(user);

        return new UserCreationResult(recoveryKey);
    }

    @Override
    @Transactional
    public boolean authenticate(String username, String password) {
        if (loginAttemptService.isBlocked(username)) {
            long remainingSeconds = loginAttemptService.getBlockTimeRemainingSeconds(username);
            long remainingMinutes = (remainingSeconds / 60) + 1;
            throw new AuthenticationException(
                    "Cuenta bloqueada por demasiados intentos fallidos. Intenta de nuevo en " + remainingMinutes + " minuto(s)."
            );
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Usuario no encontrado"));

        boolean isLegacy = user.getKeyVersion() == null || user.getKeyVersion() < 2;

        boolean valid = isLegacy
                ? encryptionService.verifyPasswordLegacy(password, user.getSalt(), user.getPasswordHash())
                : encryptionService.verifyPassword(password, user.getSalt(), user.getPasswordHash());

        if (valid) {
            loginAttemptService.loginSucceeded(username);
            encryptionService.deriveKey(password, user.getSalt());

            if (isLegacy) {
                performKeyMigration(user, password);
            }

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            userService.setCurrentUser(user);

            // Registrar login exitoso
            auditLogService.log(user,
                    com.passmanager.model.entity.AuditLog.ActionType.LOGIN,
                    "Inicio de sesión exitoso",
                    com.passmanager.model.entity.AuditLog.ResultType.SUCCESS);
        } else {
            loginAttemptService.loginFailed(username);

            // Registrar intento fallido
            auditLogService.log(user,
                    com.passmanager.model.entity.AuditLog.ActionType.LOGIN_FAILED,
                    "Intento de inicio de sesión fallido",
                    com.passmanager.model.entity.AuditLog.ResultType.FAILURE);
        }

        return valid;
    }

    /**
     * Migración v1 → v2: re-cifra todas las contraseñas del usuario de Key A a Key B,
     * calcula hmacTag con Key C, y actualiza el hash almacenado al formato v2.
     * Se ejecuta una sola vez al primer login tras el upgrade.
     */
    private void performKeyMigration(User user, String password) {
        List<PasswordEntry> entries = passwordEntryRepository.findByUserOrderByTitleAsc(user);

        for (PasswordEntry entry : entries) {
            // Re-cifrar contraseña principal: Key A → Key B
            entry.setPassword(encryptionService.migrateEncrypted(entry.getPassword()));
            entry.setHmacTag(encryptionService.sign(entry.getPassword()));

            // Re-cifrar historial de contraseñas
            for (var history : entry.getPasswordHistory()) {
                history.setPassword(encryptionService.migrateEncrypted(history.getPassword()));
            }

            // Re-cifrar campos personalizados sensibles
            for (CustomField field : entry.getCustomFields()) {
                if (field.isSensitive()) {
                    field.setFieldValue(encryptionService.migrateEncrypted(field.getFieldValue()));
                }
            }
        }
        passwordEntryRepository.saveAll(entries);

        // Actualizar hash a formato v2 (SHA-256 de Key A) y marcar versión
        user.setPasswordHash(encryptionService.hashPassword(password, user.getSalt()));
        user.setKeyVersion(2);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public boolean recoverAccount(String username, String recoveryKey, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Usuario no encontrado"));

        if (user.getRecoveryKeyHash() == null || user.getEncryptedMasterPassword() == null) {
            throw new AuthenticationException("Este usuario no tiene configurada una recovery key");
        }

        if (!recoveryKeyService.verifyRecoveryKey(recoveryKey, user.getRecoveryKeyHash())) {
            throw new AuthenticationException("Recovery key inválida");
        }

        if (newPassword.length() < 8) {
            throw new AuthenticationException("La nueva contraseña debe tener al menos 8 caracteres");
        }

        // Descifrar contraseña maestra original para re-cifrar las entradas
        String originalMasterPassword = recoveryKeyService.decryptMasterPassword(
                user.getEncryptedMasterPassword(), recoveryKey);

        // --- Fase 1: derivar claves antiguas y descifrar todo en memoria ---
        boolean wasLegacy = user.getKeyVersion() == null || user.getKeyVersion() < 2;
        encryptionService.deriveKey(originalMasterPassword, user.getSalt());

        List<PasswordEntry> entries = passwordEntryRepository.findByUserOrderByTitleAsc(user);
        Map<Long, String> entryPasswords    = new HashMap<>();
        Map<Long, String> historyPasswords  = new HashMap<>();
        Map<Long, String> sensitiveFields   = new HashMap<>();

        for (PasswordEntry entry : entries) {
            entryPasswords.put(entry.getId(),
                    wasLegacy ? encryptionService.decryptLegacy(entry.getPassword())
                              : encryptionService.decrypt(entry.getPassword()));

            for (var history : entry.getPasswordHistory()) {
                historyPasswords.put(history.getId(),
                        wasLegacy ? encryptionService.decryptLegacy(history.getPassword())
                                  : encryptionService.decrypt(history.getPassword()));
            }

            for (CustomField field : entry.getCustomFields()) {
                if (field.isSensitive()) {
                    sensitiveFields.put(field.getId(),
                            wasLegacy ? encryptionService.decryptLegacy(field.getFieldValue())
                                      : encryptionService.decrypt(field.getFieldValue()));
                }
            }
        }

        // --- Fase 2: derivar claves nuevas y re-cifrar ---
        String newSalt = encryptionService.generateSalt();
        encryptionService.deriveKey(newPassword, newSalt);

        for (PasswordEntry entry : entries) {
            entry.setPassword(encryptionService.encrypt(entryPasswords.get(entry.getId())));
            entry.setHmacTag(encryptionService.sign(entry.getPassword()));

            for (var history : entry.getPasswordHistory()) {
                history.setPassword(encryptionService.encrypt(historyPasswords.get(history.getId())));
            }

            for (CustomField field : entry.getCustomFields()) {
                if (field.isSensitive()) {
                    field.setFieldValue(encryptionService.encrypt(sensitiveFields.get(field.getId())));
                }
            }
        }
        passwordEntryRepository.saveAll(entries);

        // --- Fase 3: actualizar usuario ---
        String newRecoveryKey = recoveryKeyService.generateRecoveryKey();

        user.setPasswordHash(encryptionService.hashPassword(newPassword, newSalt));
        user.setSalt(newSalt);
        user.setKeyVersion(2);
        user.setRecoveryKeyHash(recoveryKeyService.hashRecoveryKey(newRecoveryKey));
        user.setEncryptedMasterPassword(recoveryKeyService.encryptMasterPassword(newPassword, newRecoveryKey));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        userService.setCurrentUser(user);
        loginAttemptService.loginSucceeded(username);

        return true;
    }

    @Override
    public void logout() {
        encryptionService.clearKey();
        userService.clearCurrentUser();
    }

    @Override
    public boolean isAuthenticated() {
        return encryptionService.isKeyDerived() && userService.hasCurrentUser();
    }

    @Override
    public User getCurrentUser() {
        return userService.getCurrentUser();
    }

    @Override
    public boolean verifyCurrentUserPassword(String password) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            throw new AuthenticationException("No hay un usuario autenticado");
        }

        // Simplemente verificar la contraseña contra el hash almacenado
        // NO hacemos logout/login, solo verificamos
        return encryptionService.verifyPassword(
                password,
                currentUser.getSalt(),
                currentUser.getPasswordHash()
        );
    }

    @Override
    public boolean isLoginBlocked(String username) {
        return loginAttemptService.isBlocked(username);
    }

    @Override
    public int getRemainingLoginAttempts(String username) {
        return loginAttemptService.getRemainingAttempts(username);
    }

    @Override
    public long getBlockTimeRemainingSeconds(String username) {
        return loginAttemptService.getBlockTimeRemainingSeconds(username);
    }
}
