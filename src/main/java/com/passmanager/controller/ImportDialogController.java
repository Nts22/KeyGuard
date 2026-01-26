package com.passmanager.controller;

import com.passmanager.service.BackupService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador para el diálogo de importación de contraseñas.
 *
 * <h2>Flujo de Importación</h2>
 * 1. Usuario selecciona archivo de backup (.json)
 * 2. Usuario ingresa contraseña de backup
 * 3. Usuario decide si reemplazar contraseñas existentes
 * 4. [Opcional] Usuario puede validar primero (verifica contraseña y muestra info)
 * 5. Sistema importa las contraseñas
 * 6. Sistema muestra resumen: importadas, omitidas, errores
 *
 * <h2>Dos modos de importación</h2>
 *
 * <h3>Modo Agregar (replaceExisting=false)</h3>
 * - Mantiene todas las contraseñas actuales
 * - Solo agrega las que no existan (comparación por título)
 * - Más seguro: no pierde datos existentes
 * - Recomendado para la mayoría de usuarios
 *
 * <h3>Modo Reemplazar (replaceExisting=true)</h3>
 * - ELIMINA todas las contraseñas actuales primero
 * - Importa todo del backup
 * - Útil para: restaurar desde cero, migrar a nuevo dispositivo
 * - PELIGROSO: requiere confirmación adicional
 *
 * <h2>Validación previa</h2>
 * - Permite validar el backup sin importarlo
 * - Verifica la contraseña
 * - Muestra información: versión, fecha, número de entradas
 * - Ayuda a evitar errores antes de importar
 *
 * @author KeyGuard Team
 */
@Component
@org.springframework.context.annotation.Scope("prototype")
public class ImportDialogController implements Initializable {

    @FXML private TextField filePathField;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button togglePasswordButton;

    @FXML private CheckBox replaceExistingCheckbox;

    @FXML private VBox backupInfoContainer;
    @FXML private Label backupInfoLabel;

    @FXML private Label errorLabel;
    @FXML private Button importButton;

    private final BackupService backupService;
    private Stage dialogStage;
    private boolean passwordVisible = false;
    private Runnable onImportCallback;

    public ImportDialogController(BackupService backupService) {
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
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnImportCallback(Runnable callback) {
        this.onImportCallback = callback;
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
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Backup");

        // Filtro para archivos JSON
        FileChooser.ExtensionFilter jsonFilter =
                new FileChooser.ExtensionFilter("Archivo JSON (*.json)", "*.json");
        fileChooser.getExtensionFilters().add(jsonFilter);

        // Directorio inicial: home del usuario
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
            // Ocultar info anterior y deshabilitar botón de importar
            backupInfoContainer.setVisible(false);
            backupInfoContainer.setManaged(false);
            importButton.setDisable(true);
        }
    }

    /**
     * Valida el backup sin importarlo.
     *
     * ¿Por qué ofrecer validación separada?
     * - Permite verificar la contraseña sin modificar datos
     * - Muestra información del backup (fecha, número de entradas)
     * - Ayuda al usuario a confirmar que tiene el archivo correcto
     * - Evita errores de "contraseña incorrecta" después de eliminar datos
     */
    @FXML
    private void handleValidate() {
        hideError();

        String filePath = filePathField.getText().trim();
        String password = getPasswordValue();

        if (filePath.isEmpty()) {
            showError("Debes seleccionar un archivo de backup");
            return;
        }

        if (password.isEmpty()) {
            showError("Debes ingresar la contraseña de backup");
            return;
        }

        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            showError("El archivo seleccionado no existe");
            return;
        }

        try {
            // Validar el backup
            BackupService.BackupInfo info = backupService.validateBackup(password, inputFile);

            // Mostrar información
            backupInfoLabel.setText(
                    "Versión: " + info.getVersion() + "\n" +
                    "Fecha de exportación: " + info.getExportDate() + "\n" +
                    "Número de contraseñas: " + info.getEntryCount() + "\n" +
                    "Versión de KeyGuard: " + info.getAppVersion()
            );

            backupInfoContainer.setVisible(true);
            backupInfoContainer.setManaged(true);

            // Habilitar botón de importar
            importButton.setDisable(false);

        } catch (BackupService.BackupException e) {
            showError("Error al validar backup: " + e.getMessage());
            backupInfoContainer.setVisible(false);
            backupInfoContainer.setManaged(false);
            importButton.setDisable(true);
        }
    }

    @FXML
    private void handleImport() {
        hideError();

        String filePath = filePathField.getText().trim();
        String password = getPasswordValue();
        boolean replaceExisting = replaceExistingCheckbox.isSelected();

        if (filePath.isEmpty()) {
            showError("Debes seleccionar un archivo de backup");
            return;
        }

        if (password.isEmpty()) {
            showError("Debes ingresar la contraseña de backup");
            return;
        }

        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            showError("El archivo seleccionado no existe");
            return;
        }

        // Si replaceExisting, pedir confirmación adicional
        if (replaceExisting) {
            Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
            confirmAlert.setTitle("⚠️ Confirmación Requerida");
            confirmAlert.setHeaderText("Vas a ELIMINAR todas tus contraseñas actuales");
            confirmAlert.setContentText(
                    "Esta acción no se puede deshacer.\n\n" +
                    "Se eliminarán TODAS las contraseñas guardadas actualmente\n" +
                    "y se reemplazarán con las del backup.\n\n" +
                    "¿Estás completamente seguro de continuar?"
            );

            ButtonType confirmButton = new ButtonType("Sí, eliminar todo e importar", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("No, cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(confirmButton, cancelButton);

            if (confirmAlert.showAndWait().orElse(cancelButton) != confirmButton) {
                return;
            }
        }

        // Importar
        try {
            BackupService.ImportResult result = backupService.importPasswords(password, inputFile, replaceExisting);

            // Mostrar resultado
            Alert resultAlert = new Alert(Alert.AlertType.INFORMATION);
            resultAlert.setTitle("Importación Completada");
            resultAlert.setHeaderText("Contraseñas importadas");

            StringBuilder message = new StringBuilder();
            message.append("✅ Importación completada\n\n");
            message.append("Total en backup: ").append(result.getTotalEntries()).append("\n");
            message.append("Importadas: ").append(result.getImportedEntries()).append("\n");
            message.append("Omitidas (duplicadas): ").append(result.getSkippedEntries()).append("\n");

            if (result.hasErrors()) {
                message.append("\n⚠️ Errores: ").append(result.getErrors().size()).append("\n");
                message.append("\nDetalles de errores:\n");
                for (String error : result.getErrors()) {
                    message.append("• ").append(error).append("\n");
                }
                resultAlert.setAlertType(Alert.AlertType.WARNING);
            }

            resultAlert.setContentText(message.toString());
            resultAlert.showAndWait();

            // Notificar al MainController para recargar
            if (onImportCallback != null) {
                onImportCallback.run();
            }

            dialogStage.close();

        } catch (BackupService.BackupException e) {
            showError("Error al importar: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private String getPasswordValue() {
        return passwordField.isVisible() ? passwordField.getText() : passwordVisibleField.getText();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }
}
