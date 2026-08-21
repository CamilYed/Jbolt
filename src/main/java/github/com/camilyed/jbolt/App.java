package github.com.camilyed.jbolt;

import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.infrastructure.http.HttpInfrastructure;
import github.com.camilyed.jbolt.ui.MainController;
import github.com.camilyed.jbolt.ui.RequestTabController;
import github.com.camilyed.jbolt.ui.model.RequestTabViewModel;
import github.com.camilyed.jbolt.ui.service.ComponentViewLoader;
import github.com.camilyed.jbolt.ui.service.UiMessageService;
import github.com.camilyed.jbolt.ui.service.ViewLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class App extends Application {

    private RequestExecutionService requestExecutionService;
    private UiMessageService uiMessageService;
    private ViewLoader viewLoader;

    @Override
    public void init() {
        final var httpEngine = HttpInfrastructure.defaultEngine();
        this.requestExecutionService = new RequestExecutionService(httpEngine);
        this.uiMessageService = new UiMessageService();
        this.viewLoader = new ComponentViewLoader(this::newRequestTabController);
    }

    @Override
    public void start(final Stage stage) {
        final var mainController = new MainController(viewLoader, uiMessageService);
        final var root = mainController.build();
        mainController.initialize();

        final var scene = new Scene(root, 1200, 800);
        stage.setTitle("JBolt");
        stage.setScene(scene);
        stage.show();
    }

    private RequestTabController newRequestTabController() {
        return new RequestTabController(new RequestTabViewModel(requestExecutionService));
    }

    public static void main(final String[] args) {
        launch(args);
    }
}
