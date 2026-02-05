package com.passmanager.controller;

import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.service.AuthService;
import com.passmanager.service.CategoryService;
import com.passmanager.service.PasswordEntryService;
import com.passmanager.service.ThemeService;
import com.passmanager.util.ClipboardUtil;
import com.passmanager.util.DialogUtil;
import com.passmanager.util.FxmlLoaderUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class MainController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label searchResultsLabel;
    @FXML private TableView<PasswordEntryDTO> passwordTable;
    @FXML private TableColumn<PasswordEntryDTO, String> titleColumn;
    @FXML private TableColumn<PasswordEntryDTO, String> usernameColumn;
    @FXML private TableColumn<PasswordEntryDTO, String> emailColumn;
    @FXML private TableColumn<PasswordEntryDTO, String> categoryColumn;
    @FXML private TableColumn<PasswordEntryDTO, Void> actionsColumn;
    @FXML private VBox categoriesContainer;
    @FXML private Button allCategoriesBtn;
    @FXML private Label currentUserLabel;
    @FXML private Label userInitialsLabel;
    @FXML private VBox sidebar;
    @FXML private Button toggleSidebarBtn;
    @FXML private ComboBox<ThemeService.Theme> themeSelector;
    @FXML private Pagination pagination;
    @FXML private Label autoLockTimerLabel;
    @FXML private Button manualLockBtn;

    private boolean sidebarVisible = true;
    private static final int ITEMS_PER_PAGE = 20;

    private final PasswordEntryService passwordEntryService;
    private final CategoryService categoryService;
    private final AuthService authService;
    private final ClipboardUtil clipboardUtil;
    private final DialogUtil dialogUtil;
    private final FxmlLoaderUtil fxmlLoaderUtil;
    private final com.passmanager.service.InactivityService inactivityService;
    private final com.passmanager.service.LockService lockService;
    private final ThemeService themeService;

    private ObservableList<PasswordEntryDTO> passwordList = FXCollections.observableArrayList();
    private List<PasswordEntryDTO> allPasswords = new ArrayList<>();
    private Long selectedCategoryId = null;
    private boolean isLocked = false;
    private Stage primaryStage; // Almacenar referencia al stage principal
    private javafx.animation.Timeline lockTimerUpdater; // Timeline para actualizar contador visual

    public MainController(PasswordEntryService passwordEntryService,
                          CategoryService categoryService,
                          AuthService authService,
                          ClipboardUtil clipboardUtil,
                          DialogUtil dialogUtil,
                          FxmlLoaderUtil fxmlLoaderUtil,
                          com.passmanager.service.InactivityService inactivityService,
                          com.passmanager.service.LockService lockService,
                          ThemeService themeService) {
        this.passwordEntryService = passwordEntryService;
        this.categoryService = categoryService;
        this.authService = authService;
        this.clipboardUtil = clipboardUtil;
        this.dialogUtil = dialogUtil;
        this.fxmlLoaderUtil = fxmlLoaderUtil;
        this.inactivityService = inactivityService;
        this.lockService = lockService;
        this.themeService = themeService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCurrentUser();
        setupTable();
        setupPagination();
        loadCategories();
        loadPasswords();
        setupInactivityMonitoring();
        setupLockMonitoring();
        setupThemeSelector();
    }

    private void loadCurrentUser() {
        String username = authService.getCurrentUser().getUsername();
        currentUserLabel.setText(username);
        userInitialsLabel.setText(getInitials(username));
    }

    private String getInitials(String username) {
        if (username == null || username.isEmpty()) {
            return "?";
        }
        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private void setupTable() {
        // Configurar columnas responsivas - se expanden para llenar todo el espacio
        passwordTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Solo minWidth para evitar que se hagan muy pequeñas
        titleColumn.setMinWidth(100);
        usernameColumn.setMinWidth(80);
        emailColumn.setMinWidth(120);
        categoryColumn.setMinWidth(80);
        actionsColumn.setMinWidth(180);
        actionsColumn.setPrefWidth(200);

        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCategoryName() != null ? data.getValue().getCategoryName() : "-"));

        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("Ver");
            private final Button copyBtn = new Button("Copiar");
            private final Button editBtn = new Button("Editar");
            private final Button deleteBtn = new Button("X");
            private final HBox buttons = new HBox(5, viewBtn, copyBtn, editBtn, deleteBtn);

            {
                // Alinear botones a la derecha
                buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                viewBtn.getStyleClass().addAll("table-action-btn", "table-action-btn-view");
                copyBtn.getStyleClass().addAll("table-action-btn", "table-action-btn-copy");
                editBtn.getStyleClass().addAll("table-action-btn", "table-action-btn-edit");
                deleteBtn.getStyleClass().addAll("table-action-btn", "table-action-btn-delete");

                // Tooltips para mostrar la descripción al pasar el mouse
                viewBtn.setTooltip(new Tooltip("Ver detalle de la entrada"));
                copyBtn.setTooltip(new Tooltip("Copiar contraseña al portapapeles"));
                editBtn.setTooltip(new Tooltip("Editar esta entrada"));
                deleteBtn.setTooltip(new Tooltip("Eliminar esta entrada"));

                viewBtn.setOnAction(event -> {
                    PasswordEntryDTO entry = getTableView().getItems().get(getIndex());
                    openPasswordDetail(entry);
                });

                copyBtn.setOnAction(event -> {
                    PasswordEntryDTO entry = getTableView().getItems().get(getIndex());
                    clipboardUtil.copyToClipboardWithAutoClear(entry.getPassword());
                    showNotification("Contraseña copiada (se borrará en " + clipboardUtil.getClearDelaySeconds() + "s)");
                });

                editBtn.setOnAction(event -> {
                    PasswordEntryDTO entry = getTableView().getItems().get(getIndex());
                    openPasswordForm(entry);
                });

                deleteBtn.setOnAction(event -> {
                    PasswordEntryDTO entry = getTableView().getItems().get(getIndex());
                    confirmDelete(entry);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

        passwordTable.setItems(passwordList);

        // Doble-click para abrir detalle
        passwordTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                PasswordEntryDTO selected = passwordTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openPasswordDetail(selected);
                }
            }
        });
    }

    private void loadCategories() {
        categoriesContainer.getChildren().clear();
        List<CategoryDTO> categories = categoryService.findAll();

        for (CategoryDTO category : categories) {
            // Contenedor HBox para la categoría con botón de eliminar
            HBox categoryRow = new HBox();
            categoryRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            categoryRow.getStyleClass().add("category-row");

            // Agregar icono a la categoría
            String icon = getCategoryIcon(category.getName());
            String buttonText = icon + " " + category.getName() + " (" + category.getEntryCount() + ")";

            Button categoryBtn = new Button(buttonText);
            categoryBtn.getStyleClass().addAll("category-item", "category-item-with-icon");
            categoryBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(categoryBtn, javafx.scene.layout.Priority.ALWAYS);
            categoryBtn.setOnAction(e -> selectCategory(category.getId(), categoryBtn));

            // Botón de eliminar (oculto por defecto, visible al hover)
            Button deleteBtn = new Button("×");
            deleteBtn.getStyleClass().add("category-delete-btn");
            deleteBtn.setTooltip(new Tooltip("Eliminar categoría"));
            deleteBtn.setVisible(false);
            deleteBtn.setOnAction(e -> handleDeleteCategory(category));

            // Mostrar/ocultar botón eliminar al pasar el mouse
            categoryRow.setOnMouseEntered(e -> deleteBtn.setVisible(true));
            categoryRow.setOnMouseExited(e -> deleteBtn.setVisible(false));

            // Menú contextual para editar
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Editar");
            editItem.setOnAction(e -> handleEditCategory(category));
            contextMenu.getItems().add(editItem);
            categoryBtn.setContextMenu(contextMenu);

            categoryRow.getChildren().addAll(categoryBtn, deleteBtn);
            categoriesContainer.getChildren().add(categoryRow);
        }
    }

    /**
     * Obtiene un icono apropiado basado en el nombre de la categoría
     */
    private String getCategoryIcon(String categoryName) {
        if (categoryName == null) {
            return "📁";
        }

        String lowerName = categoryName.toLowerCase();

        // Bancos y finanzas
        if (lowerName.contains("banco") || lowerName.contains("bank") ||
            lowerName.contains("tarjeta") || lowerName.contains("card") ||
            lowerName.contains("finanz") || lowerName.contains("financ")) {
            return "🏦";
        }

        // Email
        if (lowerName.contains("email") || lowerName.contains("correo") ||
            lowerName.contains("mail")) {
            return "📧";
        }

        // Social media
        if (lowerName.contains("social") || lowerName.contains("facebook") ||
            lowerName.contains("twitter") || lowerName.contains("instagram") ||
            lowerName.contains("linkedin") || lowerName.contains("tiktok")) {
            return "📱";
        }

        // Trabajo
        if (lowerName.contains("trabajo") || lowerName.contains("work") ||
            lowerName.contains("office") || lowerName.contains("empresa")) {
            return "💼";
        }

        // Compras
        if (lowerName.contains("compra") || lowerName.contains("shopping") ||
            lowerName.contains("tienda") || lowerName.contains("store") ||
            lowerName.contains("amazon") || lowerName.contains("mercado")) {
            return "🛒";
        }

        // Entretenimiento
        if (lowerName.contains("entretenimiento") || lowerName.contains("entertainment") ||
            lowerName.contains("netflix") || lowerName.contains("spotify") ||
            lowerName.contains("gaming") || lowerName.contains("juego")) {
            return "🎮";
        }

        // Viajes
        if (lowerName.contains("viaje") || lowerName.contains("travel") ||
            lowerName.contains("hotel") || lowerName.contains("vuelo")) {
            return "✈️";
        }

        // Educación
        if (lowerName.contains("educaci") || lowerName.contains("education") ||
            lowerName.contains("escuela") || lowerName.contains("school") ||
            lowerName.contains("universidad") || lowerName.contains("university")) {
            return "🎓";
        }

        // Salud
        if (lowerName.contains("salud") || lowerName.contains("health") ||
            lowerName.contains("medic") || lowerName.contains("hospital")) {
            return "🏥";
        }

        // Personal
        if (lowerName.contains("personal")) {
            return "👤";
        }

        // Default
        return "📁";
    }

    private void handleEditCategory(CategoryDTO category) {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/category-form.fxml");
            Parent formView = loader.load();

            CategoryFormController formController = loader.getController();
            formController.setCategory(category);
            formController.setOnSaveCallback(this::loadCategories);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Editar Categoría");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(formView);
            themeService.applyToScene(scene);
            attachActivityListeners(scene);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            formController.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void handleDeleteCategory(CategoryDTO category) {
        // Verificar si la categoría tiene entradas
        String message = category.getEntryCount() > 0
                ? "Esta categoría tiene " + category.getEntryCount() + " entrada(s). Las entradas quedarán sin categoría."
                : "¿Eliminar la categoría \"" + category.getName() + "\"?";

        boolean confirmed = dialogUtil.showConfirmDialog(
                passwordTable.getScene().getWindow(),
                "Eliminar Categoría",
                "¿Eliminar \"" + category.getName() + "\"?",
                message
        );

        if (confirmed) {
            categoryService.delete(category.getId());
            // Si la categoría eliminada era la seleccionada, volver a "Todas"
            if (category.getId().equals(selectedCategoryId)) {
                selectedCategoryId = null;
                updateCategorySelection(allCategoriesBtn);
            }
            loadCategories();
            loadPasswords();
        }
    }

    @FXML
    private void handleToggleSidebar() {
        sidebarVisible = !sidebarVisible;
        sidebar.setVisible(sidebarVisible);
        sidebar.setManaged(sidebarVisible);
        toggleSidebarBtn.setText(sidebarVisible ? "☰" : "☰");
    }

    private void setupPagination() {
        pagination.setPageFactory(this::createPage);
    }

    private VBox createPage(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, allPasswords.size());

        passwordList.setAll(allPasswords.subList(fromIndex, toIndex));

        return new VBox();
    }

    private void loadPasswords() {
        List<PasswordEntryDTO> entries;
        String search = searchField.getText();
        boolean isSearching = search != null && !search.isEmpty();

        if (selectedCategoryId == null) {
            entries = isSearching
                    ? passwordEntryService.search(search)
                    : passwordEntryService.findAll();
        } else {
            entries = isSearching
                    ? passwordEntryService.searchByCategory(selectedCategoryId, search)
                    : passwordEntryService.findByCategory(selectedCategoryId);
        }

        allPasswords = entries;

        // Actualizar indicador de resultados de búsqueda
        updateSearchResultsLabel(isSearching, entries.size());

        int pageCount = (int) Math.ceil((double) allPasswords.size() / ITEMS_PER_PAGE);
        pagination.setPageCount(Math.max(1, pageCount));

        // Forzar actualización de la tabla
        int currentPage = pagination.getCurrentPageIndex();
        if (currentPage == 0) {
            // Si ya estamos en página 0, forzar refresh manualmente
            createPage(0);
        } else {
            // Si estamos en otra página, ir a página 0 (esto dispara el callback automáticamente)
            pagination.setCurrentPageIndex(0);
        }
    }

    /**
     * Actualiza el label que muestra los resultados de búsqueda.
     */
    private void updateSearchResultsLabel(boolean isSearching, int resultsCount) {
        if (!isSearching) {
            searchResultsLabel.setVisible(false);
            return;
        }

        searchResultsLabel.setVisible(true);
        if (resultsCount == 0) {
            searchResultsLabel.setText("0 resultados encontrados");
            searchResultsLabel.setStyle("-fx-text-fill: #ef4444;");
        } else if (resultsCount == 1) {
            searchResultsLabel.setText("1 resultado encontrado");
            searchResultsLabel.setStyle("-fx-text-fill: #22c55e;");
        } else {
            searchResultsLabel.setText(resultsCount + " resultados encontrados");
            searchResultsLabel.setStyle("-fx-text-fill: #22c55e;");
        }
    }

    /**
     * Recarga TODAS las categorías y contraseñas.
     * Útil después de operaciones que pueden crear nuevas categorías (como importar backups).
     */
    private void reloadAll() {
        loadCategories();
        loadPasswords();
    }

    @FXML
    private void handleSearch() {
        loadPasswords();
    }

    @FXML
    private void handleAllCategories() {
        selectedCategoryId = null;
        updateCategorySelection(allCategoriesBtn);
        loadPasswords();
    }

    private void selectCategory(Long categoryId, Button button) {
        selectedCategoryId = categoryId;
        updateCategorySelection(button);
        loadPasswords();
    }

    private void updateCategorySelection(Button selectedButton) {
        allCategoriesBtn.getStyleClass().remove("category-item-selected");
        categoriesContainer.getChildren().forEach(node -> {
            if (node instanceof HBox) {
                // Las categorías ahora están en HBox, buscar el botón dentro
                ((HBox) node).getChildren().forEach(child -> {
                    if (child instanceof Button) {
                        child.getStyleClass().remove("category-item-selected");
                    }
                });
            } else if (node instanceof Button) {
                node.getStyleClass().remove("category-item-selected");
            }
        });
        selectedButton.getStyleClass().add("category-item-selected");
    }

    @FXML
    private void handleNewEntry() {
        openPasswordForm(null);
    }

    @FXML
    private void handleNewCategory() {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/category-form.fxml");
            Parent formView = loader.load();

            CategoryFormController formController = loader.getController();
            formController.setOnSaveCallback(this::loadCategories);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nueva Categoría");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(formView);
            themeService.applyToScene(scene);
            attachActivityListeners(scene);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            formController.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void openPasswordDetail(PasswordEntryDTO entry) {
        try {
            // Cargar la entrada completa con campos personalizados
            PasswordEntryDTO fullEntry = passwordEntryService.findById(entry.getId()).orElse(entry);

            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/password-detail.fxml");
            Parent detailView = loader.load();

            PasswordDetailController detailController = loader.getController();
            detailController.setEntry(fullEntry);
            detailController.setOnEditCallback(() -> openPasswordForm(fullEntry));
            detailController.setOnViewHistoryCallback(() -> handleViewHistory(fullEntry));

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Detalle: " + entry.getTitle());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(detailView);
            themeService.applyToScene(scene);
            attachActivityListeners(scene);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            detailController.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            // Recargar datos por si se editó
            loadPasswords();
            loadCategories();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el detalle: " + e.getMessage());
        }
    }

    /**
     * Abre el diálogo de historial de contraseñas para una entrada.
     *
     * Muestra todas las versiones anteriores de la contraseña con:
     * - Fecha de cambio
     * - Botones para copiar y mostrar cada versión
     * - Solo mantiene las últimas 10 versiones
     */
    public void handleViewHistory(PasswordEntryDTO entry) {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/password-history-dialog.fxml");
            Parent historyView = loader.load();

            PasswordHistoryDialogController controller = loader.getController();
            controller.setPasswordEntry(entry);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Historial: " + entry.getTitle());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(historyView);
            themeService.applyToScene(scene);
            attachActivityListeners(scene);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el historial: " + e.getMessage());
        }
    }

    private void openPasswordForm(PasswordEntryDTO entry) {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/password-form.fxml");
            Parent formView = loader.load();

            PasswordFormController formController = loader.getController();
            formController.setEntry(entry);
            formController.setOnSaveCallback(() -> {
                loadPasswords();
                loadCategories();
            });

            Stage dialogStage = new Stage();
            dialogStage.setTitle(entry == null ? "Nueva Entrada" : "Editar Entrada");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(formView);
            themeService.applyToScene(scene);
            attachActivityListeners(scene); // Resetear timer de inactividad al escribir en el formulario
            dialogStage.setScene(scene);
            dialogStage.setMinWidth(550);
            dialogStage.setMinHeight(500);

            formController.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void confirmDelete(PasswordEntryDTO entry) {
        boolean confirmed = dialogUtil.showDeleteConfirmDialog(
                passwordTable.getScene().getWindow(),
                entry.getTitle()
        );

        if (confirmed) {
            passwordEntryService.delete(entry.getId());
            loadPasswords();
            loadCategories();
        }
    }

    private void showNotification(String message) {
        dialogUtil.showNotification(passwordTable.getScene().getWindow(), message);
    }

    private void showError(String title, String message) {
        dialogUtil.showErrorDialog(passwordTable.getScene().getWindow(), title, message);
    }

    /**
     * Abre el diálogo de verificación masiva de contraseñas comprometidas.
     *
     * ¿Por qué ofrecer esta opción manual?
     * - El usuario puede querer auditar todas sus contraseñas periódicamente
     * - Complementa la verificación automática al crear/editar
     * - Permite detectar contraseñas que fueron guardadas antes de implementar esta feature
     * - Proporciona un reporte completo del estado de seguridad
     *
     * ¿Cuándo debería el usuario ejecutar esto?
     * - Después de noticias de grandes brechas de seguridad
     * - Periódicamente (cada 3-6 meses)
     * - Cuando sospeche que alguna cuenta fue comprometida
     * - Primera vez que usa la aplicación (auditar contraseñas existentes)
     */
    @FXML
    private void handleCheckBreaches() {
        try {
            // Cargar el FXML del diálogo de verificación
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/breach-check.fxml");
            Parent breachCheckView = loader.load();

            // Obtener el controlador
            BreachCheckController controller = loader.getController();

            // Crear el Stage del diálogo
            Stage breachCheckStage = new Stage();
            breachCheckStage.setTitle("Verificación de Contraseñas Filtradas");
            breachCheckStage.initModality(Modality.APPLICATION_MODAL);
            breachCheckStage.initOwner(passwordTable.getScene().getWindow());

            // Configurar la escena
            Scene scene = new Scene(breachCheckView);
            themeService.applyToScene(scene);
            attachActivityListeners(scene);
            breachCheckStage.setScene(scene);
            breachCheckStage.setResizable(false);

            // Pasar el stage al controlador
            controller.setDialogStage(breachCheckStage);

            // Iniciar la verificación automáticamente al abrir el diálogo
            // ¿Por qué iniciar automáticamente?
            // - El usuario ya hizo clic en "Verificar Contraseñas", su intención es clara
            // - No necesita hacer clic adicional
            // - Mejor UX: acción inmediata
            controller.startVerification();

            // Mostrar el diálogo
            breachCheckStage.showAndWait();

            // Después de cerrar, recargar las contraseñas por si el usuario cambió alguna
            loadPasswords();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir la verificación de contraseñas: " + e.getMessage());
        }
    }

    /**
     * Abre el diálogo para exportar contraseñas a un backup cifrado.
     *
     * ¿Por qué ofrecer exportación?
     * - Protección contra pérdida de datos (disco dañado, borrado accidental)
     * - Migración a otro dispositivo
     * - Compartir contraseñas de forma segura (ej: contraseñas familiares)
     * - Cumplimiento de políticas de backup
     *
     * ¿Por qué cifrado?
     * - Permite almacenar backup en nube (Dropbox, Google Drive)
     * - Protege si el archivo cae en manos incorrectas
     * - Compatible con principio zero-knowledge
     */
    @FXML
    private void handleExport() {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/export-dialog.fxml");
            Parent exportView = loader.load();

            ExportDialogController controller = loader.getController();

            Stage exportStage = new Stage();
            exportStage.setTitle("Exportar Contraseñas");
            exportStage.initModality(Modality.APPLICATION_MODAL);
            exportStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(exportView);
            themeService.applyToScene(scene);
            attachActivityListeners(scene);
            exportStage.setScene(scene);
            exportStage.setResizable(false);

            controller.setDialogStage(exportStage);
            exportStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el diálogo de exportación: " + e.getMessage());
        }
    }

    /**
     * Abre el diálogo para importar contraseñas desde un backup cifrado.
     *
     * ¿Por qué ofrecer importación?
     * - Restaurar desde backup después de pérdida de datos
     * - Migrar desde otro dispositivo
     * - Importar contraseñas compartidas
     *
     * ¿Dos modos?
     * - Agregar: Mantiene contraseñas actuales, solo agrega nuevas
     * - Reemplazar: Elimina todo y restaura desde backup
     *
     * ¿Por qué validación previa?
     * - Verifica contraseña sin modificar datos
     * - Muestra info del backup (fecha, cantidad)
     * - Evita errores de "contraseña incorrecta" después de eliminar datos
     */
    @FXML
    private void handleImport() {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/import-dialog.fxml");
            Parent importView = loader.load();

            ImportDialogController controller = loader.getController();

            Stage importStage = new Stage();
            importStage.setTitle("Importar Contraseñas");
            importStage.initModality(Modality.APPLICATION_MODAL);
            importStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(importView);
            themeService.applyToScene(scene);
            attachActivityListeners(scene);
            importStage.setScene(scene);
            importStage.setResizable(false);

            controller.setDialogStage(importStage);

            // Callback para recargar categorías y contraseñas después de importar
            // (pueden haberse creado nuevas categorías automáticamente)
            controller.setOnImportCallback(this::reloadAll);

            importStage.showAndWait();

            // Recargar todo después de cerrar (por si importó)
            reloadAll();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el diálogo de importación: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        boolean confirmed = dialogUtil.showConfirmDialog(
                passwordTable.getScene().getWindow(),
                "Cerrar Sesión",
                "¿Cerrar sesión?",
                "Volverás a la pantalla de inicio de sesión."
        );

        if (confirmed) {
            try {
                // Detener monitoreo de inactividad
                inactivityService.stopMonitoring();

                authService.logout();

                FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/login.fxml");
                Parent loginView = loader.load();

                Stage stage = (Stage) passwordTable.getScene().getWindow();
                Scene scene = new Scene(loginView);
                themeService.applyToScene(scene);
                stage.setScene(scene);
                stage.setTitle("KeyGuard - Login");
                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error", "No se pudo cerrar sesión: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExit() {
        boolean confirmed = dialogUtil.showConfirmDialog(
                passwordTable.getScene().getWindow(),
                "Salir",
                "¿Salir de la aplicación?",
                "Se cerrarán todas las ventanas."
        );

        if (confirmed) {
            javafx.application.Platform.exit();
        }
    }

    private void setupInactivityMonitoring() {
        inactivityService.startMonitoring(this::handleInactivityLogout);
    }

    private void onActivityDetected() {
        inactivityService.resetTimer();
        lockService.resetTimer();
    }

    /**
     * Adjunta listeners de actividad a una Scene para resetear timers de inactividad/bloqueo.
     * Usar esto en diálogos para que la actividad en ellos también cuente como actividad del usuario.
     */
    private void attachActivityListeners(Scene scene) {
        scene.setOnMouseMoved(event -> onActivityDetected());
        scene.setOnMousePressed(event -> onActivityDetected());
        scene.setOnMouseClicked(event -> onActivityDetected());
        scene.setOnKeyPressed(event -> onActivityDetected());
        scene.setOnKeyReleased(event -> onActivityDetected());
        scene.setOnScroll(event -> onActivityDetected());
    }

    private void handleInactivityLogout() {
        // Detener monitoreo
        inactivityService.stopMonitoring();

        // Mostrar mensaje al usuario
        javafx.application.Platform.runLater(() -> {
            try {
                // Mostrar alerta de sesión cerrada
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                alert.setTitle("Sesión Cerrada");
                alert.setHeaderText("Sesión cerrada por inactividad");
                alert.setContentText("Tu sesión ha sido cerrada automáticamente por 3 minutos de inactividad.");
                alert.initOwner(passwordTable.getScene().getWindow());
                alert.showAndWait();

                // Cerrar sesión
                authService.logout();

                // Navegar al login
                FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/login.fxml");
                Parent loginView = loader.load();

                Stage stage = (Stage) passwordTable.getScene().getWindow();
                Scene scene = new Scene(loginView);
                themeService.applyToScene(scene);
                stage.setScene(scene);
                stage.setTitle("KeyGuard - Login");
                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Maneja el bloqueo manual de la bóveda cuando el usuario hace clic en el botón.
     */
    @FXML
    private void handleManualLock() {
        System.out.println("Bloqueo manual activado por el usuario");
        lockService.lockNow();
    }

    /**
     * Inicia el Timeline que actualiza el contador visual de auto-lock cada segundo.
     */
    private void startLockTimerUpdater() {
        // Detener Timeline anterior si existe
        if (lockTimerUpdater != null) {
            lockTimerUpdater.stop();
        }

        // Crear nuevo Timeline que se ejecuta cada segundo
        lockTimerUpdater = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                event -> updateLockTimerDisplay()
            )
        );
        lockTimerUpdater.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        lockTimerUpdater.play();

        // Actualizar inmediatamente
        updateLockTimerDisplay();
    }

    /**
     * Actualiza el label del contador de auto-lock con el tiempo restante.
     */
    private void updateLockTimerDisplay() {
        if (lockService == null || autoLockTimerLabel == null) {
            return;
        }

        try {
            // Calcular tiempo restante basado en el timeout configurado
            int timeoutMinutes = lockService.getInactivityTimeout();

            // Mostrar el tiempo configurado en formato corto
            if (timeoutMinutes == 1) {
                autoLockTimerLabel.setText("⏱️ 1 min");
            } else {
                autoLockTimerLabel.setText("⏱️ " + timeoutMinutes + " min");
            }
        } catch (Exception e) {
            autoLockTimerLabel.setText("⏱️ Activo");
        }
    }

    /**
     * Detiene el Timeline del contador visual.
     */
    private void stopLockTimerUpdater() {
        if (lockTimerUpdater != null) {
            lockTimerUpdater.stop();
            lockTimerUpdater = null;
        }
    }

    /**
     * Configura el monitoreo de bloqueo automático.
     * Se activa por minimización de ventana o inactividad (2 min).
     */
    private void setupLockMonitoring() {
        // Iniciar monitoreo de bloqueo con callback
        lockService.startMonitoring(this::handleLock);

        // Configurar timeouts
        lockService.setInactivityTimeout(2); // 2 minutos
        lockService.setLockOnMinimize(true);

        // Agregar listeners para eventos de ventana y actividad
        javafx.application.Platform.runLater(() -> {
            if (passwordTable.getScene() != null) {
                Stage stage = (Stage) passwordTable.getScene().getWindow();

                // Detectar minimización de ventana
                stage.iconifiedProperty().addListener((obs, wasIconified, isNowIconified) -> {
                    if (isNowIconified && lockService.isLockOnMinimizeEnabled()) {
                        System.out.println("Ventana minimizada - bloqueando aplicación");
                        lockService.lockNow();
                    }
                });

                // Resetear ambos timers (bloqueo + logout) en cualquier actividad
                passwordTable.getScene().setOnMouseMoved(event -> onActivityDetected());
                passwordTable.getScene().setOnMousePressed(event -> onActivityDetected());
                passwordTable.getScene().setOnMouseClicked(event -> onActivityDetected());
                passwordTable.getScene().setOnKeyPressed(event -> onActivityDetected());
                passwordTable.getScene().setOnKeyReleased(event -> onActivityDetected());
                passwordTable.getScene().setOnScroll(event -> onActivityDetected());

                // Iniciar actualización visual del contador
                startLockTimerUpdater();
            }
        });
    }

    private void setupThemeSelector() {
        themeSelector.setItems(FXCollections.observableArrayList(ThemeService.Theme.values()));
        themeSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemeService.Theme theme) {
                return theme != null ? theme.getDisplayName() : "";
            }

            @Override
            public ThemeService.Theme fromString(String string) {
                return null;
            }
        });
        themeSelector.setValue(themeService.getCurrentTheme());
        themeSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                themeService.setCurrentTheme(newVal);
            }
        });
    }

    /**
     * Maneja el bloqueo de la aplicación.
     * Muestra pantalla de desbloqueo en lugar de cerrar sesión.
     */
    private void handleLock() {
        if (isLocked) {
            return; // Ya está bloqueada
        }

        isLocked = true;

        javafx.application.Platform.runLater(() -> {
            try {
                // Detener monitoreo mientras está bloqueada
                lockService.stopMonitoring();

                // Detener actualización visual del contador
                stopLockTimerUpdater();

                // Almacenar referencia al stage ANTES de cambiar la escena
                primaryStage = (Stage) passwordTable.getScene().getWindow();

                // Limpiar datos sensibles de memoria
                clearSensitiveData();

                // Cargar pantalla de desbloqueo
                FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/unlock.fxml");
                Parent unlockView = loader.load();

                UnlockController controller = loader.getController();

                controller.setOnUnlockSuccess(() -> {
                    // Callback al desbloquear exitosamente
                    try {
                        // Recargar la vista principal
                        FXMLLoader mainLoader = fxmlLoaderUtil.getLoader("/fxml/main.fxml");
                        Parent mainView = mainLoader.load();

                        // Cambiar de vuelta a la vista principal
                        Scene mainScene = new Scene(mainView);
                        mainScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                        primaryStage.setScene(mainScene);
                        primaryStage.setTitle("KeyGuard - Gestor de Contraseñas");
                        primaryStage.centerOnScreen();

                        isLocked = false;
                        System.out.println("Aplicación desbloqueada correctamente");
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error al restaurar la aplicación", e.getMessage());
                    }
                });

                // Configurar callback para logout desde pantalla de bloqueo
                controller.setOnLogout(() -> {
                    handleLogoutFromLockScreen();
                });

                // Cambiar a pantalla de desbloqueo
                Scene scene = new Scene(unlockView);
                themeService.applyToScene(scene);
                primaryStage.setScene(scene);
                primaryStage.setTitle("KeyGuard - Bloqueada");
                primaryStage.centerOnScreen();

            } catch (Exception e) {
                e.printStackTrace();
                // Si falla, cerrar sesión como fallback
                handleLogout();
            }
        });
    }

    /**
     * Maneja el logout cuando se llama desde la pantalla de bloqueo.
     * Usa el primaryStage almacenado en lugar de obtenerlo de la tabla.
     */
    private void handleLogoutFromLockScreen() {
        try {
            // Detener monitoreo de inactividad y bloqueo
            if (inactivityService != null) {
                inactivityService.stopMonitoring();
            }
            if (lockService != null) {
                lockService.stopMonitoring();
            }

            authService.logout();

            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/login.fxml");
            Parent loginView = loader.load();

            Scene scene = new Scene(loginView);
            themeService.applyToScene(scene);
            primaryStage.setScene(scene);
            primaryStage.setTitle("KeyGuard - Login");
            primaryStage.centerOnScreen();

            isLocked = false;
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo cerrar sesión: " + e.getMessage());
        }
    }

    /**
     * Limpia datos sensibles de memoria antes de bloquear.
     * Previene que contraseñas queden en memoria mientras está bloqueada.
     */
    private void clearSensitiveData() {
        try {
            // Limpiar tabla de contraseñas (usar setItems para evitar problemas con listas inmutables)
            if (passwordTable != null) {
                passwordTable.setItems(FXCollections.observableArrayList());
            }

            // Limpiar listas en memoria
            if (passwordList != null) {
                try {
                    passwordList.clear();
                } catch (UnsupportedOperationException e) {
                    // Si la lista es inmutable, crear una nueva
                    passwordList = FXCollections.observableArrayList();
                }
            }
            if (allPasswords != null) {
                try {
                    allPasswords.clear();
                } catch (UnsupportedOperationException e) {
                    // Si la lista es inmutable, crear una nueva
                    allPasswords = new ArrayList<>();
                }
            }

            // Sugerir garbage collection (no garantizado pero ayuda)
            System.gc();

            System.out.println("Datos sensibles limpiados de memoria");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
