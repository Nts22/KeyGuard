package com.passmanager.controller;

import com.passmanager.service.BackupService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controlador para el diálogo de exportación de contraseñas.
 *
 * <h2>Flujo de Exportación</h2>
 * 1. Usuario ingresa contraseña de backup (mínimo 8 caracteres)
 * 2. Usuario confirma la contraseña
 * 3. Usuario selecciona ubicación del archivo
 * 4. Sistema exporta y cifra todas las contraseñas
 * 5. Sistema muestra confirmación con ubicación del archivo
 *
 * <h2>Validaciones</h2>
 * - Contraseña mínimo 8 caracteres
 * - Las contraseñas deben coincidir
 * - Debe seleccionar ubicación válida
 * - No debe sobrescribir archivo sin confirmar
 *
 * <h2>UX</h2>
 * - Toggle para mostrar/ocultar contraseñas
 * - Sugerencia automática de nombre de archivo con fecha
 * - Confirmación antes de sobrescribir archivo existente
 * - Mensaje de éxito con ubicación del backup
 *
 * @author KeyGuard Team
 */
@Component
@org.springframework.context.annotation.Scope("prototype")
public class ExportDialogController implements Initializable {

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmVisibleField;
    @FXML private Button toggleConfirmButton;

    @FXML private TextField filePathField;
    @FXML private Label errorLabel;

    private final BackupService backupService;
    private Stage dialogStage;
    private boolean passwordVisible = false;
    private boolean confirmVisible = false;

    public ExportDialogController(BackupService backupService) {
        this.backupService = backupService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Sincronizar campos de contraseña
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordVisibleField.getText().equals(newVal)) {
                passwordVisibleField.setText(newVal);
            }
        });

        passwordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordField.getText().equals(newVal)) {
                passwordField.setText(newVal);
            }
        });

        // Sincronizar campos de confirmación
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!confirmVisibleField.getText().equals(newVal)) {
                confirmVisibleField.setText(newVal);
            }
        });

        confirmVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!confirmPasswordField.getText().equals(newVal)) {
                confirmPasswordField.setText(newVal);
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void handleTogglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            togglePasswordButton.setText("🔒");
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordButton.setText("👁");
        }
    }

    @FXML
    private void handleToggleConfirm() {
        confirmVisible = !confirmVisible;
        if (confirmVisible) {
            confirmVisibleField.setText(confirmPasswordField.getText());
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            confirmVisibleField.setVisible(true);
            confirmVisibleField.setManaged(true);
            toggleConfirmButton.setText("🔒");
        } else {
            confirmPasswordField.setText(confirmVisibleField.getText());
            confirmVisibleField.setVisible(false);
            confirmVisibleField.setManaged(false);
            confirmPasswordField.setVisible(true);
            confirmPasswordField.setManaged(true);
            toggleConfirmButton.setText("👁");
        }
    }

    /**
     * Abre el diálogo para seleccionar ubicación del archivo.
     *
     * ¿Por qué sugerir nombre con fecha?
     * - Ayuda al usuario a organizar múltiples backups
     * - Evita sobrescribir backups anteriores accidentalmente
     * - Formato: keyguard-backup-2024-01-26.json
     */
    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Backup");

        // Sugerir nombre con fecha actual
        String defaultFileName = "keyguard-backup-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                ".json";
        fileChooser.setInitialFileName(defaultFileName);

        // Filtro para archivos JSON
        FileChooser.ExtensionFilter jsonFilter =
                new FileChooser.ExtensionFilter("Archivo JSON (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(jsonFilter);

        // Directorio inicial: home del usuario
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedFile = fileChooser.showSaveDialog(dialogStage);
        if (selectedFile != null) {
            // Asegurar que termine en .json
            String path = selectedFile.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".json")) {
                path += ".json";
            }
            filePathField.setText(path);
        }
    }

    @FXML
    private void handleExport() {
        hideError();

        // Validar entrada
        String password = getPasswordValue();
        String confirmPassword = getConfirmPasswordValue();
        String filePath = filePathField.getText().trim();

        if (password.isEmpty()) {
            showError("Debes ingresar una contraseña de backup");
            return;
        }

        if (password.length() < 8) {
            showError("La contraseña debe tener al menos 8 caracteres");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Las contraseñas no coinciden");
            return;
        }

        if (filePath.isEmpty()) {
            showError("Debes seleccionar una ubicación para el archivo");
            return;
        }

        File outputFile = new File(filePath);

        // Confirmar sobrescritura si el archivo ya existe
        if (outputFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Sobrescribir archivo");
            alert.setHeaderText("El archivo ya existe");
            alert.setContentText("¿Deseas sobrescribir el archivo existente?\n\n" + filePath);

            if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        // Exportar
        try {
            backupService.exportPasswords(password, outputFile);

            // Mensaje de éxito
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Exportación Exitosa");
            alert.setHeaderText("Backup creado correctamente");
            alert.setContentText(
                    "✅ Tus contraseñas han sido exportadas y cifradas.\n\n" +
                    "Ubicación:\n" + outputFile.getAbsolutePath() + "\n\n" +
                    "Guarda este archivo en un lugar seguro.\n" +
                    "Necesitarás la contraseña de backup para restaurarlo."
            );
            alert.showAndWait();

            dialogStage.close();

        } catch (BackupService.BackupException e) {
            showError("Error al exportar: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private String getPasswordValue() {
        return passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();
    }

    private String getConfirmPasswordValue() {
        return confirmPasswordField.isVisible() ? confirmPasswordField.getText() : confirmVisibleField.getText();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }
}
