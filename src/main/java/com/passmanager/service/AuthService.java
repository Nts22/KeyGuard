package com.passmanager.service;

import com.passmanager.model.dto.UserDTO;
import com.passmanager.model.entity.User;

import java.util.List;

public interface AuthService {

    boolean hasUsers();

    List<UserDTO> getAllUsers();

    /**
     * Crea un nuevo usuario con recovery key.
     *
     * @param username Nombre de usuario
     * @param password Contraseña maestra
     * @return Resultado con el usuario creado y la recovery key
     */
    UserCreationResult createUser(String username, String password);

    boolean authenticate(String username, String password);

    /**
     * Recupera el acceso a la cuenta usando una recovery key.
     *
     * @param username Nombre de usuario
     * @param recoveryKey Recovery key proporcionada por el usuario
     * @param newPassword Nueva contraseña maestra
     * @return true si la recuperación fue exitosa
     */
    boolean recoverAccount(String username, String recoveryKey, String newPassword);

    void logout();

    boolean isAuthenticated();

    User getCurrentUser();

    /**
     * Verifica la contraseña del usuario actual sin hacer logout/login.
     * Útil para desbloquear la aplicación sin perder el estado de autenticación.
     *
     * @param password Contraseña a verificar
     * @return true si la contraseña es correcta
     */
    boolean verifyCurrentUserPassword(String password);

    boolean isLoginBlocked(String username);

    int getRemainingLoginAttempts(String username);

    long getBlockTimeRemainingSeconds(String username);

    /**
     * Resultado de la creación de usuario que incluye el usuario y la recovery key.
     */
    class UserCreationResult {
        private final String recoveryKey;

        public UserCreationResult(String recoveryKey) {
            this.recoveryKey = recoveryKey;
        }

        public String getRecoveryKey() {
            return recoveryKey;
        }
    }
}
