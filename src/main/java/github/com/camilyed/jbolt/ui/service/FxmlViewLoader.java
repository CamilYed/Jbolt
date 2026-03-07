package github.com.camilyed.jbolt.ui.service;

import github.com.camilyed.jbolt.common.result.Result;
import github.com.camilyed.jbolt.ui.ControllerFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;

public final class FxmlViewLoader implements ViewLoader {
    private final ControllerFactory factory;

    public FxmlViewLoader(ControllerFactory factory) {
        this.factory = factory;
    }

    @Override
    public Result<Parent> load(final String fxmlPath) {
        return Result.of(() -> {
            final var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new IOException("FXML resource not found: " + fxmlPath);
            }
            final var loader = new FXMLLoader(resource);
            loader.setControllerFactory(factory);
            return loader.load();
        });
    }
}