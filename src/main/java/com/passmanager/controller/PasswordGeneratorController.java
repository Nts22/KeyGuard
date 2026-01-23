package com.passmanager.controller;

import com.passmanager.util.ClipboardUtil;
import com.passmanager.util.PasswordGeneratorUtil;
import com.passmanager.util.PasswordGeneratorUtil.PasswordStrength;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

@Component
@org.springframework.context.annotation.Scope("prototype")
public class PasswordGeneratorController implements Initializable {

    @FXML private TextField passwordField;
    @FXML private Slider lengthSlider;
    @FXML private Label lengthLabel;
    @FXML private CheckBox lowercaseCheck;
    @FXML private CheckBox uppercaseCheck;
    @FXML private CheckBox digitsCheck;
    @FXML private CheckBox symbolsCheck;
    @FXML private CheckBox excludeAmbiguousCheck;
    @FXML private ProgressBar strengthBar;
    @FXML private Label strengthLabel;

    private final PasswordGeneratorUtil generatorUtil;
    private final ClipboardUtil clipboardUtil;

    private Stage dialogStage;
    private Consumer<String> onSelectCallback;

    public PasswordGeneratorController(PasswordGeneratorUtil generatorUtil, ClipboardUtil clipboardUtil) {
        this.generatorUtil = generatorUtil;
        this.clipboardUtil = clipboardUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar slider
        lengthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lengthLabel.setText(String.valueOf(newVal.intValue()));
            generatePassword();
        });

        // Listeners para checkboxes
        lowercaseCheck.selectedProperty().addListener((obs, oldVal, newVal) -> generatePassword());
        uppercaseCheck.selectedProperty().addListener((obs, oldVal, newVal) -> generatePassword());
        digitsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> generatePassword());
        symbolsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> generatePassword());
        excludeAmbiguousCheck.selectedProperty().addListener((obs, oldVal, newVal) -> generatePassword());

        // Listener para cambios manuales en el campo de contraseña
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> updateStrength(newVal));

        // Generar contraseña inicial
        generatePassword();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnSelectCallback(Consumer<String> callback) {
        this.onSelectCallback = callback;
    }

    @FXML
    private void handleGenerate() {
        generatePassword();
    }

    @FXML
    private void handleCopy() {
        String password = passwordField.getText();
        if (password != null && !password.isEmpty()) {
            clipboardUtil.copyToClipboardWithAutoClear(password);
        }
    }

    @FXML
    private void handleUse() {
        String password = passwordField.getText();
        if (onSelectCallback != null && password != null && !password.isEmpty()) {
            onSelectCallback.accept(password);
        }
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void generatePassword() {
        int length = (int) lengthSlider.getValue();
        boolean lowercase = lowercaseCheck.isSelected();
        boolean uppercase = uppercaseCheck.isSelected();
        boolean digits = digitsCheck.isSelected();
        boolean symbols = symbolsCheck.isSelected();
        boolean excludeAmbiguous = excludeAmbiguousCheck.isSelected();

        // Asegurar que al menos una opción esté seleccionada
        if (!lowercase && !uppercase && !digits && !symbols) {
            lowercaseCheck.setSelected(true);
            lowercase = true;
        }

        String password = generatorUtil.generate(length, lowercase, uppercase, digits, symbols, excludeAmbiguous);
        passwordField.setText(password);
        updateStrength(password);
    }

    private void updateStrength(String password) {
        PasswordStrength strength = generatorUtil.evaluateStrength(password);
        strengthBar.setProgress(strength.getProgress());
        strengthLabel.setText(strength.getLabel());

        // Aplicar color al progress bar
        strengthBar.setStyle("-fx-accent: " + strength.getColor() + ";");
    }
}
