package com.passmanager;

import com.passmanager.config.AppConfig;
import com.passmanager.config.DatabaseInitializer;
import com.passmanager.util.FxmlLoaderUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class PasswordManagerApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Crear directorio de base de datos ANTES de iniciar Spring
        DatabaseInitializer.init();

        springContext = new SpringApplicationBuilder(SpringBootApp.class)
                .headless(false)
                .run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FxmlLoaderUtil fxmlLoaderUtil = springContext.getBean(FxmlLoaderUtil.class);
        Parent root = fxmlLoaderUtil.loadFxml("/fxml/login.fxml");

        Scene scene = new Scene(root, 400, 500);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setTitle(AppConfig.APP_NAME);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
