package com.passmanager.controller;

import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.service.PasswordBreachService;
import com.passmanager.service.PasswordEntryService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador para la verificación masiva de contraseñas comprometidas.
 *
 * <h2>¿Por qué verificación masiva?</h2>
 * - Permite al usuario auditar todas sus contraseñas de una vez
 * - Identifica contraseñas vulnerables que ya están guardadas
 * - Proporciona un reporte completo del estado de seguridad
 *
 * <h2>Desafíos de Implementación</h2>
 *
 * <h3>1. Performance</h3>
 * - No podemos verificar 100 contraseñas de forma síncrona (bloquearía la UI)
 * - Usamos JavaFX Task para procesamiento en background
 * - Actualizamos la UI progresivamente con Platform.runLater()
 *
 * <h3>2. Rate Limiting</h3>
 * - HIBP API permite requests ilimitados pero razonables
 * - Agregamos un pequeño delay entre requests (100ms) para ser respetuosos
 * - Si hacemos spam, podríamos ser bloqueados temporalmente
 *
 * <h3>3. Manejo de Errores</h3>
 * - Una contraseña que falla no debe detener toda la verificación
 * - Marcamos errores individuales y continuamos
 * - Al final mostramos cuántas fallaron y por qué
 *
 * <h3>4. UX</h3>
 * - Mostrar progreso en tiempo real (barra + texto)
 * - Permitir cancelar la operación
 * - Mostrar resultados parciales mientras se procesa
 *
 * @author KeyGuard Team
 */
@Component
@org.springframework.context.annotation.Scope("prototype")
public class BreachCheckController implements Initializable {

    @FXML private VBox progressContainer;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;

    @FXML private HBox summaryContainer;
    @FXML private Label totalLabel;
    @FXML private Label safeLabel;
    @FXML private Label breachedLabel;

    @FXML private TableView<BreachCheckResult> resultsTable;
    @FXML private TableColumn<BreachCheckResult, String> statusColumn;
    @FXML private TableColumn<BreachCheckResult, String> titleColumn;
    @FXML private TableColumn<BreachCheckResult, String> usernameColumn;
    @FXML private TableColumn<BreachCheckResult, String> severityColumn;
    @FXML private TableColumn<BreachCheckResult, String> occurrencesColumn;
    @FXML private TableColumn<BreachCheckResult, String> recommendationColumn;

    @FXML private VBox infoContainer;
    @FXML private Button exportButton;

    private final PasswordEntryService passwordEntryService;
    private final PasswordBreachService passwordBreachService;

    private Stage dialogStage;
    private Task<Void> verificationTask;
    private final ObservableList<BreachCheckResult> results = FXCollections.observableArrayList();

    public BreachCheckController(PasswordEntryService passwordEntryService,
                                  PasswordBreachService passwordBreachService) {
        this.passwordEntryService = passwordEntryService;
        this.passwordBreachService = passwordBreachService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
    }

    /**
     * Configura las columnas de la tabla de resultados.
     *
     * ¿Por qué usar PropertyValueFactory?
     * - NO lo usamos porque queremos control total sobre el formato
     * - Usamos setCellValueFactory con lambdas para personalizar cada columna
     * - Esto permite formatear números, colores, emojis, etc.
     */
    private void setupTable() {
        // Columna de estado con emoji
        statusColumn.setCellValueFactory(data -> {
            BreachCheckResult result = data.getValue();
            String emoji = result.isBreached() ? "⚠️" : "✅";
            return new SimpleStringProperty(emoji);
        });
        statusColumn.setStyle("-fx-alignment: CENTER; -fx-font-size: 18px;");

        // Título de la contraseña
        titleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEntry().getTitle()));

        // Usuario
        usernameColumn.setCellValueFactory(data -> {
            String username = data.getValue().getEntry().getUsername();
            return new SimpleStringProperty(username != null && !username.isEmpty() ? username : "-");
        });

        // Severidad con colores
        severityColumn.setCellValueFactory(data -> {
            BreachCheckResult result = data.getValue();
            if (result.getError() != null) {
                return new SimpleStringProperty("Error");
            }
            return new SimpleStringProperty(result.getSeverityLevel().getLabel());
        });

        // Customizar las celdas de severidad con colores
        severityColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Aplicar colores según severidad
                    if (item.equals("Segura")) {
                        setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    } else if (item.equals("Riesgo Bajo")) {
                        setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold;");
                    } else if (item.equals("Riesgo Medio")) {
                        setStyle("-fx-text-fill: #f97316; -fx-font-weight: bold;");
                    } else if (item.equals("Riesgo Alto")) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else if (item.equals("Riesgo Crítico")) {
                        setStyle("-fx-text-fill: #991b1b; -fx-font-weight: bold;");
                    } else if (item.equals("Error")) {
                        setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
                    }
                }
            }
        });

        // Número de apariciones
        occurrencesColumn.setCellValueFactory(data -> {
            BreachCheckResult result = data.getValue();
            if (result.getError() != null) {
                return new SimpleStringProperty("-");
            }
            if (!result.isBreached()) {
                return new SimpleStringProperty("0");
            }
            // Formatear con separador de miles para números grandes
            return new SimpleStringProperty(String.format("%,d", result.getOccurrences()));
        });
        occurrencesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Recomendación
        recommendationColumn.setCellValueFactory(data -> {
            BreachCheckResult result = data.getValue();
            if (result.getError() != null) {
                return new SimpleStringProperty("No se pudo verificar");
            }
            if (!result.isBreached()) {
                return new SimpleStringProperty("Mantener contraseña");
            }

            // Recomendación basada en severidad
            PasswordBreachService.SeverityLevel severity = result.getSeverityLevel();
            switch (severity) {
                case CRITICAL:
                    return new SimpleStringProperty("⛔ CAMBIAR INMEDIATAMENTE");
                case HIGH:
                    return new SimpleStringProperty("⚠️ Cambiar urgentemente");
                case MEDIUM:
                    return new SimpleStringProperty("⚠️ Cambiar pronto");
                case LOW:
                    return new SimpleStringProperty("ℹ️ Considerar cambio");
                default:
                    return new SimpleStringProperty("Revisar");
            }
        });

        resultsTable.setItems(results);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;

        // Cuando se cierre el diálogo, cancelar la tarea si está en progreso
        dialogStage.setOnCloseRequest(event -> {
            if (verificationTask != null && verificationTask.isRunning()) {
                verificationTask.cancel();
            }
        });
    }

    /**
     * Inicia la verificación masiva de contraseñas.
     *
     * ¿Por qué usar un Task?
     * - Task es la forma correcta de hacer operaciones largas en JavaFX
     * - Corre en un thread separado (no bloquea la UI)
     * - Proporciona progress tracking automático
     * - Permite cancelación
     * - Integración con Platform.runLater() para actualizar UI
     */
    public void startVerification() {
        // Obtener todas las contraseñas del usuario actual
        List<PasswordEntryDTO> allPasswords = passwordEntryService.findAll();

        if (allPasswords.isEmpty()) {
            progressLabel.setText("No hay contraseñas para verificar");
            return;
        }

        // Crear la tarea de verificación
        verificationTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int total = allPasswords.size();
                int processed = 0;
                int safeCount = 0;
                int breachedCount = 0;

                for (PasswordEntryDTO entry : allPasswords) {
                    // Verificar si la tarea fue cancelada
                    if (isCancelled()) {
                        updateMessage("Verificación cancelada");
                        break;
                    }

                    // Actualizar mensaje de progreso
                    updateMessage("Verificando: " + entry.getTitle() + " (" + (processed + 1) + "/" + total + ")");

                    try {
                        // Verificar la contraseña
                        PasswordBreachService.BreachCheckResult breachResult =
                                passwordBreachService.checkPassword(entry.getPassword());

                        // Crear resultado
                        BreachCheckResult result = new BreachCheckResult(
                                entry,
                                breachResult.isBreached(),
                                breachResult.getOccurrences(),
                                breachResult.getSeverityLevel(),
                                null
                        );

                        // Agregar a la lista de resultados (en el thread de UI)
                        Platform.runLater(() -> results.add(result));

                        // Actualizar contadores
                        if (breachResult.isBreached()) {
                            breachedCount++;
                        } else {
                            safeCount++;
                        }

                        // ⏱️ DELAY ENTRE REQUESTS
                        // ¿Por qué esperar 100ms entre requests?
                        // - Ser respetuosos con la API de HIBP
                        // - Evitar rate limiting
                        // - 100ms es imperceptible para el usuario pero ayuda al servidor
                        // - Para 50 contraseñas = 5 segundos extra (aceptable)
                        Thread.sleep(100);

                    } catch (PasswordBreachService.PasswordBreachCheckException e) {
                        // Error al verificar esta contraseña específica
                        BreachCheckResult errorResult = new BreachCheckResult(
                                entry,
                                false,
                                0,
                                PasswordBreachService.SeverityLevel.SAFE,
                                e.getMessage()
                        );

                        Platform.runLater(() -> results.add(errorResult));
                    }

                    // Actualizar progreso
                    processed++;
                    updateProgress(processed, total);

                    // Actualizar contadores en la UI
                    final int finalSafeCount = safeCount;
                    final int finalBreachedCount = breachedCount;
                    final int finalProcessed = processed;
                    Platform.runLater(() -> {
                        totalLabel.setText(String.valueOf(finalProcessed));
                        safeLabel.setText(String.valueOf(finalSafeCount));
                        breachedLabel.setText(String.valueOf(finalBreachedCount));
                    });
                }

                // Verificación completada
                updateMessage("Verificación completada: " + processed + " contraseñas analizadas");
                return null;
            }
        };

        // Vincular el progreso de la tarea con la barra de progreso
        progressBar.progressProperty().bind(verificationTask.progressProperty());
        progressLabel.textProperty().bind(verificationTask.messageProperty());

        // Cuando la tarea termine (exitosa o cancelada)
        verificationTask.setOnSucceeded(event -> onVerificationComplete());
        verificationTask.setOnCancelled(event -> onVerificationComplete());
        verificationTask.setOnFailed(event -> {
            progressLabel.setText("Error: " + verificationTask.getException().getMessage());
            onVerificationComplete();
        });

        // Iniciar la tarea en un thread separado
        Thread thread = new Thread(verificationTask);
        thread.setDaemon(true); // Daemon thread para que no impida cerrar la app
        thread.start();

        // Mostrar UI de progreso
        progressContainer.setVisible(true);
        summaryContainer.setVisible(true);
        resultsTable.setVisible(true);
        infoContainer.setVisible(true);
    }

    /**
     * Callback cuando la verificación se completa.
     */
    private void onVerificationComplete() {
        // Desvincular el progreso
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();

        // Completar la barra de progreso
        progressBar.setProgress(1.0);

        // Habilitar botón de exportar si hay resultados
        if (!results.isEmpty()) {
            exportButton.setVisible(true);
            exportButton.setDisable(false);
        }

        // Mostrar alerta si hay contraseñas comprometidas
        long breachedCount = results.stream()
                .filter(BreachCheckResult::isBreached)
                .count();

        if (breachedCount > 0) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Contraseñas Comprometidas Detectadas");
                alert.setHeaderText("Se encontraron " + breachedCount + " contraseña(s) comprometida(s)");
                alert.setContentText(
                        "Se recomienda cambiar estas contraseñas lo antes posible.\n\n" +
                        "Las contraseñas comprometidas han sido filtradas en brechas de seguridad " +
                        "y son conocidas por hackers. Cambiarlas reducirá significativamente " +
                        "el riesgo de que tus cuentas sean comprometidas."
                );
                alert.showAndWait();
            });
        }
    }

    @FXML
    private void handleExport() {
        // TODO: Implementar exportación de resultados a CSV
        // Por ahora solo mostramos un mensaje
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exportar Resultados");
        alert.setHeaderText("Función en desarrollo");
        alert.setContentText("La exportación de resultados estará disponible en una próxima versión.");
        alert.showAndWait();
    }

    @FXML
    private void handleClose() {
        // Cancelar tarea si está en progreso
        if (verificationTask != null && verificationTask.isRunning()) {
            verificationTask.cancel();
        }
        dialogStage.close();
    }

    /**
     * Clase para almacenar el resultado de verificación de una contraseña.
     */
    public static class BreachCheckResult {
        private final PasswordEntryDTO entry;
        private final boolean breached;
        private final int occurrences;
        private final PasswordBreachService.SeverityLevel severityLevel;
        private final String error;

        public BreachCheckResult(PasswordEntryDTO entry, boolean breached, int occurrences,
                                 PasswordBreachService.SeverityLevel severityLevel, String error) {
            this.entry = entry;
            this.breached = breached;
            this.occurrences = occurrences;
            this.severityLevel = severityLevel;
            this.error = error;
        }

        public PasswordEntryDTO getEntry() {
            return entry;
        }

        public boolean isBreached() {
            return breached;
        }

        public int getOccurrences() {
            return occurrences;
        }

        public PasswordBreachService.SeverityLevel getSeverityLevel() {
            return severityLevel;
        }

        public String getError() {
            return error;
        }
    }
}
