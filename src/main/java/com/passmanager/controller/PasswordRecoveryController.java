package com.passmanager.controller;

import com.passmanager.config.AppConfig;
import com.passmanager.model.dto.UserDTO;
import com.passmanager.service.AuthService;
import com.passmanager.util.FxmlLoaderUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class PasswordRecoveryController implements Initializable {

    @FXML private ComboBox<UserDTO> userCombo;
    @FXML private TextField recoveryKeyField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button recoverButton;
    @FXML private Button backButton;

    private final AuthService authService;
    private final FxmlLoaderUtil fxmlLoaderUtil;

    public PasswordRecoveryController(AuthService authService,
                                     FxmlLoaderUtil fxmlLoaderUtil) {
        this.authService = authService;
        this.fxmlLoaderUtil = fxmlLoaderUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUserCombo();
        setupEnterKeyHandlers();
    }

    private void setupUserCombo() {
        userCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserDTO user) {
                return user != null ? user.getUsername() : "";
            }

            @Override
            public UserDTO fromString(String string) {
                return null;
            }
        });

        List<UserDTO> users = authService.getAllUsers();
        userCombo.setItems(FXCollections.observableArrayList(users));
        if (!users.isEmpty()) {
            userCombo.setValue(users.get(0));
        }
    }

    private void setupEnterKeyHandlers() {
        recoveryKeyField.setOnAction(event -> newPasswordField.requestFocus());
        newPasswordField.setOnAction(event -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(event -> handleRecover());
    }

    @FXML
    private void handleRecover() {
        hideMessage();

        // Validaciones
        UserDTO selectedUser = userCombo.getValue();
        String recoveryKey = recoveryKeyField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (selectedUser == null) {
            showError("Selecciona un usuario");
            return;
        }

        if (recoveryKey.isEmpty()) {
            showError("Ingresa tu clave de recuperación");
            return;
        }

        if (newPassword.isEmpty()) {
            showError("Ingresa una nueva contraseña");
            return;
        }

        if (newPassword.length() < 8) {
            showError("La contraseña debe tener al menos 8 caracteres");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Las contraseñas no coinciden");
            return;
        }

        // Intentar recuperar cuenta
        try {
            boolean success = authService.recoverAccount(
                selectedUser.getUsername(),
                recoveryKey,
                newPassword
            );

            if (success) {
                showSuccess("¡Cuenta recuperada exitosamente!");
                // Esperar 1 segundo antes de navegar
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(this::navigateToMain);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        navigateToLogin();
    }

    private void navigateToMain() {
        try {
            Parent mainView = fxmlLoaderUtil.loadFxml("/fxml/main.fxml");
            Scene scene = new Scene(mainView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) recoverButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(AppConfig.APP_NAME);
            stage.setResizable(true);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Error al cargar la pantalla principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToLogin() {
        try {
            Parent loginView = fxmlLoaderUtil.loadFxml("/fxml/login.fxml");
            Scene scene = new Scene(loginView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(AppConfig.APP_NAME);
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Error al cargar el login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #dc3545;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }
}
