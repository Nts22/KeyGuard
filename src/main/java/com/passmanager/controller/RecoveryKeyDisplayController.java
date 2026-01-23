package com.passmanager.controller;

import com.passmanager.config.AppConfig;
import com.passmanager.service.CategoryService;
import com.passmanager.util.ClipboardUtil;
import com.passmanager.util.FxmlLoaderUtil;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class RecoveryKeyDisplayController {

    @FXML private TextField recoveryKeyField;
    @FXML private Button copyButton;
    @FXML private CheckBox confirmCheckbox;
    @FXML private Label errorLabel;
    @FXML private Button continueButton;

    private final ClipboardUtil clipboardUtil;
    private final CategoryService categoryService;
    private final FxmlLoaderUtil fxmlLoaderUtil;

    private String recoveryKey;

    public RecoveryKeyDisplayController(ClipboardUtil clipboardUtil,
                                        CategoryService categoryService,
                                        FxmlLoaderUtil fxmlLoaderUtil) {
        this.clipboardUtil = clipboardUtil;
        this.categoryService = categoryService;
        this.fxmlLoaderUtil = fxmlLoaderUtil;
    }

    /**
     * Establece la recovery key a mostrar.
     * Este método debe ser llamado después de cargar el FXML.
     */
    public void setRecoveryKey(String recoveryKey) {
        this.recoveryKey = recoveryKey;
        if (recoveryKeyField != null) {
            recoveryKeyField.setText(recoveryKey);
        }
    }

    @FXML
    private void initialize() {
        if (recoveryKey != null) {
            recoveryKeyField.setText(recoveryKey);
        }
    }

    @FXML
    private void handleCopy() {
        if (recoveryKey != null && !recoveryKey.isEmpty()) {
            clipboardUtil.copyToClipboard(recoveryKey);
            copyButton.setText("✓ Copiado");
            copyButton.setDisable(true);

            // Reactivar botón después de 2 segundos
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> {
                        copyButton.setText("📋 Copiar al Portapapeles");
                        copyButton.setDisable(false);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    @FXML
    private void handleContinue() {
        hideError();

        // Validar que el usuario haya confirmado
        if (!confirmCheckbox.isSelected()) {
            showError("Debes confirmar que guardaste tu clave de recuperación");
            return;
        }

        try {
            // Inicializar categorías predeterminadas
            categoryService.initDefaultCategories();

            // Navegar a la pantalla principal
            navigateToMain();
        } catch (Exception e) {
            showError("Error al inicializar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void navigateToMain() {
        try {
            Parent mainView = fxmlLoaderUtil.loadFxml("/fxml/main.fxml");
            Scene scene = new Scene(mainView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) continueButton.getScene().getWindow();
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

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
