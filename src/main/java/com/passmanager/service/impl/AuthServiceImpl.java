package com.passmanager.service.impl;

import com.passmanager.exception.AuthenticationException;
import com.passmanager.model.dto.UserDTO;
import com.passmanager.model.entity.User;
import com.passmanager.repository.UserRepository;
import com.passmanager.service.AuthService;
import com.passmanager.service.EncryptionService;
import com.passmanager.service.LoginAttemptService;
import com.passmanager.service.RecoveryKeyService;
import com.passmanager.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final EncryptionService encryptionService;
    private final LoginAttemptService loginAttemptService;
    private final RecoveryKeyService recoveryKeyService;

    public AuthServiceImpl(UserRepository userRepository,
                           UserService userService,
                           EncryptionService encryptionService,
                           LoginAttemptService loginAttemptService,
                           RecoveryKeyService recoveryKeyService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.encryptionService = encryptionService;
        this.loginAttemptService = loginAttemptService;
        this.recoveryKeyService = recoveryKeyService;
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
                .build();

        user = userRepository.save(user);

        encryptionService.deriveKey(password, salt);
        userService.setCurrentUser(user);

        return new UserCreationResult(user, recoveryKey);
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

        boolean valid = encryptionService.verifyPassword(
                password,
                user.getSalt(),
                user.getPasswordHash()
        );

        if (valid) {
            loginAttemptService.loginSucceeded(username);
            encryptionService.deriveKey(password, user.getSalt());
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            userService.setCurrentUser(user);
        } else {
            loginAttemptService.loginFailed(username);
        }

        return valid;
    }

    @Override
    @Transactional
    public boolean recoverAccount(String username, String recoveryKey, String newPassword) {
        // Buscar usuario
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Usuario no encontrado"));

        // Verificar que el usuario tenga recovery key configurada
        if (user.getRecoveryKeyHash() == null || user.getEncryptedMasterPassword() == null) {
            throw new AuthenticationException("Este usuario no tiene configurada una recovery key");
        }

        // Verificar recovery key
        if (!recoveryKeyService.verifyRecoveryKey(recoveryKey, user.getRecoveryKeyHash())) {
            throw new AuthenticationException("Recovery key inválida");
        }

        // Validar nueva contraseña
        if (newPassword.length() < 8) {
            throw new AuthenticationException("La nueva contraseña debe tener al menos 8 caracteres");
        }

        // Descifrar contraseña maestra original (para re-cifrar las contraseñas guardadas)
        String originalMasterPassword = recoveryKeyService.decryptMasterPassword(
            user.getEncryptedMasterPassword(),
            recoveryKey
        );

        // Generar nuevo salt y hash para la nueva contraseña
        String newSalt = encryptionService.generateSalt();
        String newHash = encryptionService.hashPassword(newPassword, newSalt);

        // Generar nueva recovery key
        String newRecoveryKey = recoveryKeyService.generateRecoveryKey();
        String newRecoveryKeyHash = recoveryKeyService.hashRecoveryKey(newRecoveryKey);
        String newEncryptedMasterPassword = recoveryKeyService.encryptMasterPassword(newPassword, newRecoveryKey);

        // Actualizar usuario
        user.setPasswordHash(newHash);
        user.setSalt(newSalt);
        user.setRecoveryKeyHash(newRecoveryKeyHash);
        user.setEncryptedMasterPassword(newEncryptedMasterPassword);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Derivar clave y establecer usuario actual
        encryptionService.deriveKey(newPassword, newSalt);
        userService.setCurrentUser(user);

        // Limpiar intentos de login fallidos
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
