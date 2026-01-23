package com.passmanager.util;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FxmlLoaderUtil {

    private final ApplicationContext applicationContext;

    public FxmlLoaderUtil(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <T> T loadFxml(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(applicationContext::getBean);
        return loader.load();
    }

    public FXMLLoader getLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(applicationContext::getBean);
        return loader;
    }
}
