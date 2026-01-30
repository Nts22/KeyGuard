package com.passmanager.controller;

import com.passmanager.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controlador para la pantalla de desbloqueo.
 *
 * <h2>Flujo de desbloqueo</h2>
 * 1. Usuario ingresa su contraseña maestra
 * 2. Se valida contra la BD
 * 3. Si es correcta → desbloquear y volver a MainController
 * 4. Si es incorrecta → mostrar error y permitir reintentar
 *
 * <h2>Seguridad</h2>
 * - NO limita intentos (usuario ya autenticado previamente)
 * - Contraseña se limpia de memoria después de validar
 * - No muestra información sensible en pantalla
 *
 * @author KeyGuard Team
 */
@Component
@org.springframework.context.annotation.Scope("prototype")
public class UnlockController implements Initializable {

    @FXML private PasswordField passwordField;
    @FXML private Label usernameLabel;
    @FXML private Label lockedTimeLabel;
    @FXML private Label errorLabel;

    private final AuthService authService;
    private Stage dialogStage;
    private Runnable onUnlockSuccess;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public UnlockController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar Enter para desbloquear
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleUnlock();
            }
        });

        // Mostrar usuario actual
        try {
            String username = authService.getCurrentUser().getUsername();
            usernameLabel.setText(username);
        } catch (Exception e) {
            usernameLabel.setText("Usuario");
        }

        // Mostrar hora de bloqueo
        String lockedTime = LocalDateTime.now().format(TIME_FORMATTER);
        lockedTimeLabel.setText("Bloqueado a las " + lockedTime);

        // Enfocar campo de contraseña
        javafx.application.Platform.runLater(() -> passwordField.requestFocus());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;

        // Prevenir cierre con X o ESC
        dialogStage.setOnCloseRequest(event -> {
            event.consume(); // No permitir cerrar
        });
    }

    public void setOnUnlockSuccess(Runnable callback) {
        this.onUnlockSuccess = callback;
    }

    @FXML
    private void handleUnlock() {
        String password = passwordField.getText();

        // Validar que no esté vacío
        if (password == null || password.isEmpty()) {
            showError("Ingresa tu contraseña");
            return;
        }

        // Validar contraseña
        try {
            String username = authService.getCurrentUser().getUsername();
            boolean isValid = authService.authenticate(username, password);

            if (isValid) {
                // Contraseña correcta - desbloquear
                clearError();
                passwordField.clear();

                // Cerrar diálogo
                if (dialogStage != null) {
                    dialogStage.close();
                }

                // Ejecutar callback de éxito
                if (onUnlockSuccess != null) {
                    javafx.application.Platform.runLater(onUnlockSuccess);
                }
            } else {
                // Contraseña incorrecta
                showError("Contraseña incorrecta");
                passwordField.clear();
                passwordField.requestFocus();
            }
        } catch (Exception e) {
            showError("Error al desbloquear: " + e.getMessage());
            e.printStackTrace();
        }

        // Limpiar contraseña de memoria
        password = null;
    }

    @FXML
    private void handleLogout() {
        // Confirmar antes de cerrar sesión
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION
        );
        alert.setTitle("Cerrar Sesión");
        alert.setHeaderText("¿Cerrar sesión?");
        alert.setContentText("Volverás a la pantalla de inicio de sesión.");
        alert.initOwner(dialogStage);

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                // Usuario confirmó - cerrar sesión completa
                if (dialogStage != null) {
                    dialogStage.close();
                }

                // Ejecutar logout
                javafx.application.Platform.runLater(() -> {
                    try {
                        authService.logout();
                        // El callback onUnlockSuccess debería manejar la navegación al login
                        if (onUnlockSuccess != null) {
                            onUnlockSuccess.run();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
