package com.passmanager.controller;

import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.model.dto.PasswordHistoryDTO;
import com.passmanager.service.PasswordHistoryService;
import com.passmanager.util.ClipboardUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador para el diálogo de historial de contraseñas.
 *
 * Muestra todas las versiones anteriores de una contraseña con:
 * - Fecha de cambio
 * - Contraseña oculta por defecto
 * - Botones para copiar y mostrar cada versión
 */
@Component
@org.springframework.context.annotation.Scope("prototype")
public class PasswordHistoryDialogController implements Initializable {

    @FXML private TableView<PasswordHistoryDTO> historyTable;
    @FXML private TableColumn<PasswordHistoryDTO, String> passwordColumn;
    @FXML private TableColumn<PasswordHistoryDTO, String> dateColumn;
    @FXML private TableColumn<PasswordHistoryDTO, Void> actionsColumn;
    @FXML private Label titleLabel;
    @FXML private Label countLabel;

    private final PasswordHistoryService historyService;
    private final ClipboardUtil clipboardUtil;
    private Stage dialogStage;
    private PasswordEntryDTO passwordEntry;

    public PasswordHistoryDialogController(PasswordHistoryService historyService,
                                          ClipboardUtil clipboardUtil) {
        this.historyService = historyService;
        this.clipboardUtil = clipboardUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurar columna de fecha
        dateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFormattedDate()));
        dateColumn.setPrefWidth(200);

        // Configurar columna de contraseña (oculta por defecto)
        passwordColumn.setCellValueFactory(data ->
                new SimpleStringProperty("••••••••••••"));
        passwordColumn.setPrefWidth(250);

        // Configurar columna de acciones
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button copyBtn = new Button("📋 Copiar");
            private final Button showBtn = new Button("👁");
            private final HBox box = new HBox(8, showBtn, copyBtn);
            private boolean passwordVisible = false;

            {
                box.setAlignment(Pos.CENTER);
                copyBtn.getStyleClass().add("button-copy");
                showBtn.getStyleClass().add("button-copy");

                copyBtn.setOnAction(e -> {
                    PasswordHistoryDTO item = getTableView().getItems().get(getIndex());
                    clipboardUtil.copyToClipboardWithAutoClear(item.getPassword());
                    showCopiedFeedback(copyBtn);
                });

                showBtn.setOnAction(e -> {
                    passwordVisible = !passwordVisible;
                    PasswordHistoryDTO item = getTableView().getItems().get(getIndex());

                    if (passwordVisible) {
                        // Mostrar contraseña en un Alert
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Contraseña Anterior");
                        alert.setHeaderText("Contraseña del " + item.getFormattedDate());
                        alert.setContentText(item.getPassword());
                        alert.initOwner(dialogStage);
                        alert.showAndWait();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
        actionsColumn.setPrefWidth(150);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setPasswordEntry(PasswordEntryDTO entry) {
        this.passwordEntry = entry;
        loadHistory();
        titleLabel.setText("Historial: " + entry.getTitle());
    }

    private void loadHistory() {
        List<PasswordHistoryDTO> history = historyService.getHistory(passwordEntry.getId());
        historyTable.getItems().setAll(history);

        String countText = history.isEmpty()
            ? "No hay versiones anteriores"
            : history.size() + " versión(es) anterior(es)";
        countLabel.setText(countText);
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }

    private void showCopiedFeedback(Button button) {
        String originalText = button.getText();
        button.setText("✓");
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                javafx.application.Platform.runLater(() -> button.setText(originalText));
            } catch (InterruptedException ignored) {}
        }).start();
    }
}
