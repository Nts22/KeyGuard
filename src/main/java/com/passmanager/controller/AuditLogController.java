package com.passmanager.controller;

import com.passmanager.model.entity.AuditLog;
import com.passmanager.model.entity.AuditLog.ActionType;
import com.passmanager.model.entity.AuditLog.ResultType;
import com.passmanager.model.entity.User;
import com.passmanager.service.AuditLogService;
import com.passmanager.service.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class AuditLogController implements Initializable {

    @FXML private TableView<AuditLog> auditTable;
    @FXML private TableColumn<AuditLog, String> timestampColumn;
    @FXML private TableColumn<AuditLog, String> actionColumn;
    @FXML private TableColumn<AuditLog, String> resultColumn;
    @FXML private TableColumn<AuditLog, String> descriptionColumn;
    @FXML private TableColumn<AuditLog, String> ipAddressColumn;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private Label totalEntriesLabel;
    @FXML private Button closeBtn;
    @FXML private javafx.scene.control.Pagination pagination;

    private final AuditLogService auditLogService;
    private final UserService userService;

    private ObservableList<AuditLog> auditList = FXCollections.observableArrayList();
    private List<AuditLog> allAuditLogs;
    private List<AuditLog> filteredLogs; // Lista filtrada para la paginación

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int ITEMS_PER_PAGE = 10;

    public AuditLogController(AuditLogService auditLogService, UserService userService) {
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupFilter();
        setupPagination();
        loadAuditLogs();
    }

    private void setupTable() {
        timestampColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getTimestamp().format(DATE_FORMATTER)));

        actionColumn.setCellValueFactory(data ->
            new SimpleStringProperty(formatActionType(data.getValue().getAction())));

        resultColumn.setCellValueFactory(data ->
            new SimpleStringProperty(formatResult(data.getValue().getResult())));

        descriptionColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDescription() != null ? data.getValue().getDescription() : "-"));

        ipAddressColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getIpAddress() != null ? data.getValue().getIpAddress() : "-"));

        // Estilo personalizado para la columna de resultado
        resultColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("✓")) {
                        setStyle("-fx-text-fill: #10b981;");
                    } else if (item.contains("✕")) {
                        setStyle("-fx-text-fill: #ef4444;");
                    } else if (item.contains("🚫")) {
                        setStyle("-fx-text-fill: #f59e0b;");
                    }
                }
            }
        });

        auditTable.setItems(auditList);
    }

    private void setupFilter() {
        filterComboBox.setItems(FXCollections.observableArrayList(
            "Todas las acciones",
            "Solo inicios de sesión",
            "Solo acciones en contraseñas",
            "Solo exportaciones/importaciones",
            "Solo errores"
        ));
        filterComboBox.setValue("Todas las acciones");
        filterComboBox.setOnAction(e -> applyFilter());
    }

    private void setupPagination() {
        pagination.setPageFactory(pageIndex -> {
            updatePage(pageIndex);
            return new javafx.scene.layout.VBox(); // Retorna un nodo dummy, no se usa
        });
    }

    private void loadAuditLogs() {
        User currentUser = userService.getCurrentUser();
        allAuditLogs = auditLogService.findByUser(currentUser);
        filteredLogs = allAuditLogs; // Inicializar con todos los registros
        applyFilter();
    }

    private void applyFilter() {
        String filter = filterComboBox.getValue();

        switch (filter) {
            case "Solo inicios de sesión":
                filteredLogs = allAuditLogs.stream()
                    .filter(log -> log.getAction() == ActionType.LOGIN ||
                                   log.getAction() == ActionType.LOGIN_FAILED ||
                                   log.getAction() == ActionType.LOGOUT)
                    .toList();
                break;
            case "Solo acciones en contraseñas":
                filteredLogs = allAuditLogs.stream()
                    .filter(log -> log.getAction() == ActionType.CREATE_ENTRY ||
                                   log.getAction() == ActionType.UPDATE_ENTRY ||
                                   log.getAction() == ActionType.DELETE_ENTRY ||
                                   log.getAction() == ActionType.VIEW_PASSWORD ||
                                   log.getAction() == ActionType.COPY_PASSWORD ||
                                   log.getAction() == ActionType.REVEAL_PASSWORD)
                    .toList();
                break;
            case "Solo exportaciones/importaciones":
                filteredLogs = allAuditLogs.stream()
                    .filter(log -> log.getAction() == ActionType.EXPORT_VAULT ||
                                   log.getAction() == ActionType.IMPORT_VAULT ||
                                   log.getAction() == ActionType.BACKUP_CREATED)
                    .toList();
                break;
            case "Solo errores":
                filteredLogs = allAuditLogs.stream()
                    .filter(log -> log.getResult() == ResultType.FAILURE ||
                                   log.getResult() == ResultType.BLOCKED)
                    .toList();
                break;
            default:
                filteredLogs = allAuditLogs;
        }

        // Actualizar paginación
        int pageCount = (int) Math.ceil((double) filteredLogs.size() / ITEMS_PER_PAGE);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);

        totalEntriesLabel.setText(filteredLogs.size() + " entradas");

        // Mostrar primera página
        updatePage(0);
    }

    private void updatePage(int pageIndex) {
        if (filteredLogs == null || filteredLogs.isEmpty()) {
            auditList.clear();
            return;
        }

        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filteredLogs.size());

        if (fromIndex < filteredLogs.size()) {
            List<AuditLog> pageItems = filteredLogs.subList(fromIndex, toIndex);
            auditList.setAll(pageItems);
        } else {
            auditList.clear();
        }
    }

    private String formatActionType(ActionType action) {
        return switch (action) {
            case LOGIN -> "🔓 Iniciar sesión";
            case LOGIN_FAILED -> "❌ Inicio de sesión fallido";
            case LOGOUT -> "🚪 Cerrar sesión";
            case UNLOCK -> "🔓 Desbloquear bóveda";
            case ACCOUNT_LOCKED -> "🔒 Cuenta bloqueada";
            case CREATE_ENTRY -> "➕ Crear contraseña";
            case UPDATE_ENTRY -> "✏️ Actualizar contraseña";
            case DELETE_ENTRY -> "🗑️ Eliminar contraseña";
            case VIEW_PASSWORD -> "👁️ Ver contraseña";
            case COPY_PASSWORD -> "📋 Copiar contraseña";
            case REVEAL_PASSWORD -> "👁️ Revelar contraseña";
            case EXPORT_VAULT -> "💾 Exportar bóveda";
            case IMPORT_VAULT -> "📥 Importar bóveda";
            case BACKUP_CREATED -> "💾 Backup creado";
            case TOTP_ENABLED -> "🔐 Habilitar 2FA";
            case TOTP_DISABLED -> "🔓 Deshabilitar 2FA";
            case TOTP_VERIFIED -> "✓ Verificar 2FA";
            case CREATE_CATEGORY -> "📁 Crear categoría";
            case DELETE_CATEGORY -> "🗑️ Eliminar categoría";
            case CREATE_TAG -> "🏷️ Crear tag";
            case DELETE_TAG -> "🗑️ Eliminar tag";
            case RECOVERY_KEY_GENERATED -> "🔑 Generar clave de recuperación";
            case RECOVERY_KEY_USED -> "🔄 Usar clave de recuperación";
            case BREACH_CHECK_RUN -> "🔍 Verificar brechas";
            case PASSWORD_GENERATOR_USED -> "🎲 Generador de contraseñas usado";
            default -> action.name();
        };
    }

    private String formatResult(ResultType result) {
        return switch (result) {
            case SUCCESS -> "✓ Éxito";
            case FAILURE -> "✕ Fallo";
            case BLOCKED -> "🚫 Bloqueado";
        };
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }
}
