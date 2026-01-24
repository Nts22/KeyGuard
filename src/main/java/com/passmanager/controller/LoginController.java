package com.passmanager.controller;

import com.passmanager.config.AppConfig;
import com.passmanager.model.dto.UserDTO;
import com.passmanager.service.AuthService;
import com.passmanager.service.CategoryService;
import com.passmanager.util.FxmlLoaderUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class LoginController implements Initializable {

    @FXML private ComboBox<UserDTO> userCombo;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmVisibleField;
    @FXML private VBox confirmContainer;
    @FXML private HBox usernameContainer;
    @FXML private HBox userSelectContainer;
    @FXML private Label subtitleLabel;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;
    @FXML private Button toggleModeButton;
    @FXML private Button forgotPasswordButton;
    @FXML private Button togglePasswordButton;
    @FXML private Button toggleConfirmButton;

    private final AuthService authService;
    private final CategoryService categoryService;
    private final FxmlLoaderUtil fxmlLoaderUtil;

    private boolean isRegisterMode;
    private boolean hasUsers;

    public LoginController(AuthService authService, CategoryService categoryService,
                           FxmlLoaderUtil fxmlLoaderUtil) {
        this.authService = authService;
        this.categoryService = categoryService;
        this.fxmlLoaderUtil = fxmlLoaderUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hasUsers = authService.hasUsers();

        if (hasUsers) {
            setupLoginMode();
        } else {
            setupRegisterMode();
        }

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

        if (hasUsers) {
            List<UserDTO> users = authService.getAllUsers();
            userCombo.setItems(FXCollections.observableArrayList(users));
            if (!users.isEmpty()) {
                userCombo.setValue(users.get(0));
            }
        }
    }

    private void setupEnterKeyHandlers() {
        passwordField.setOnAction(event -> handleSubmit());
        if (confirmPasswordField != null) {
            confirmPasswordField.setOnAction(event -> handleSubmit());
        }
        if (usernameField != null) {
            usernameField.setOnAction(event -> passwordField.requestFocus());
        }
    }

    private void setupLoginMode() {
        isRegisterMode = false;
        subtitleLabel.setText("Iniciar sesión");
        submitButton.setText("Entrar");
        toggleModeButton.setText("Crear nuevo usuario");

        showElement(userSelectContainer);
        hideElement(usernameContainer);
        hideElement(confirmContainer);
        showElement(forgotPasswordButton);
    }

    private void setupRegisterMode() {
        isRegisterMode = true;
        subtitleLabel.setText("Crear nuevo usuario");
        submitButton.setText("Crear Usuario");
        toggleModeButton.setText(hasUsers ? "Iniciar sesión" : "");
        toggleModeButton.setVisible(hasUsers);
        toggleModeButton.setManaged(hasUsers);

        hideElement(userSelectContainer);
        showElement(usernameContainer);
        showElement(confirmContainer);
        hideElement(forgotPasswordButton);
    }

    @FXML
    private void handleToggleMode() {
        hideError();
        clearFields();

        if (isRegisterMode) {
            setupLoginMode();
        } else {
            setupRegisterMode();
        }
    }

    @FXML
    private void handleSubmit() {
        hideError();

        if (isRegisterMode) {
            handleRegister();
        } else {
            handleLogin();
        }
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = getPasswordValue();
        String confirm = getConfirmPasswordValue();

        if (username.isEmpty()) {
            showError("Ingresa un nombre de usuario");
            return;
        }

        if (username.length() < 3) {
            showError("El nombre de usuario debe tener al menos 3 caracteres");
            return;
        }

        if (password.isEmpty()) {
            showError("Ingresa una contraseña");
            return;
        }

        if (password.length() < 8) {
            showError("La contraseña debe tener al menos 8 caracteres");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Las contraseñas no coinciden");
            return;
        }

        try {
            AuthService.UserCreationResult result = authService.createUser(username, password);
            navigateToRecoveryKeyDisplay(result.getRecoveryKey());
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void handleLogin() {
        UserDTO selectedUser = userCombo.getValue();
        String password = getPasswordValue();

        if (selectedUser == null) {
            showError("Selecciona un usuario");
            return;
        }

        if (password.isEmpty()) {
            showError("Ingresa la contraseña");
            return;
        }

        String username = selectedUser.getUsername();

        if (authService.isLoginBlocked(username)) {
            long remainingSeconds = authService.getBlockTimeRemainingSeconds(username);
            long remainingMinutes = (remainingSeconds / 60) + 1;
            showError("Cuenta bloqueada. Intenta de nuevo en " + remainingMinutes + " minuto(s).");
            return;
        }

        try {
            if (authService.authenticate(username, password)) {
                navigateToMain();
            } else {
                int remaining = authService.getRemainingLoginAttempts(username);
                if (remaining > 0) {
                    showError("Contraseña incorrecta. " + remaining + " intento(s) restante(s).");
                } else {
                    showError("Cuenta bloqueada por demasiados intentos fallidos.");
                }
                passwordField.clear();
                passwordVisibleField.clear();
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        hideError();
        navigateToPasswordRecovery();
    }

    @FXML
    private void handleTogglePasswordVisibility() {
        if (passwordField.isVisible()) {
            // Cambiar a TextField visible
            passwordVisibleField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            togglePasswordButton.setText("👁️‍🗨️");
        } else {
            // Cambiar a PasswordField oculto
            passwordField.setText(passwordVisibleField.getText());
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordButton.setText("👁");
        }
    }

    @FXML
    private void handleToggleConfirmVisibility() {
        if (confirmPasswordField.isVisible()) {
            // Cambiar a TextField visible
            confirmVisibleField.setText(confirmPasswordField.getText());
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            confirmVisibleField.setVisible(true);
            confirmVisibleField.setManaged(true);
            toggleConfirmButton.setText("👁️‍🗨️");
        } else {
            // Cambiar a PasswordField oculto
            confirmPasswordField.setText(confirmVisibleField.getText());
            confirmVisibleField.setVisible(false);
            confirmVisibleField.setManaged(false);
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            toggleConfirmButton.setText("👁");
        }
    }

    private void navigateToMain() {
        try {
            Parent mainView = fxmlLoaderUtil.loadFxml("/fxml/main.fxml");
            Scene scene = new Scene(mainView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) passwordField.getScene().getWindow();
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

    private void navigateToRecoveryKeyDisplay(String recoveryKey) {
        try {
            javafx.fxml.FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/recovery-key-display.fxml");
            Parent recoveryView = loader.load();

            // Obtener el controlador y establecer la recovery key
            RecoveryKeyDisplayController controller = loader.getController();
            controller.setRecoveryKey(recoveryKey);

            Scene scene = new Scene(recoveryView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(AppConfig.APP_NAME + " - Clave de Recuperación");
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Error al mostrar la clave de recuperación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToPasswordRecovery() {
        try {
            Parent recoveryView = fxmlLoaderUtil.loadFxml("/fxml/password-recovery.fxml");
            Scene scene = new Scene(recoveryView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(AppConfig.APP_NAME + " - Recuperar Cuenta");
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Error al cargar la pantalla de recuperación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    private void clearFields() {
        if (usernameField != null) usernameField.clear();
        passwordField.clear();
        passwordVisibleField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        if (confirmVisibleField != null) confirmVisibleField.clear();
    }

    private void showElement(javafx.scene.Node node) {
        if (node != null) {
            node.setVisible(true);
            node.setManaged(true);
        }
    }

    private void hideElement(javafx.scene.Node node) {
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
        }
    }

    private String getPasswordValue() {
        return passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();
    }

    private String getConfirmPasswordValue() {
        return confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmVisibleField.getText();
    }
}
