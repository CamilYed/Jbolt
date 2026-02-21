package github.com.camilyed.jbolt;

import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.infrastructure.http.HttpInfrastructure;
import github.com.camilyed.jbolt.ui.ControllerFactory;
import github.com.camilyed.jbolt.ui.MainController;
import github.com.camilyed.jbolt.ui.RequestTabController;
import github.com.camilyed.jbolt.ui.model.RequestTabViewModel;
import github.com.camilyed.jbolt.ui.service.UiMessageService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class App extends Application {

    private RequestExecutionService requestExecutionService;
    private UiMessageService uiMessageService;

    @Override
    public void init() {
        final var httpEngine = HttpInfrastructure.defaultEngine();
        this.requestExecutionService = new RequestExecutionService(httpEngine);
        this.uiMessageService = new UiMessageService();
    }

    @Override
    public void start(final Stage stage) throws Exception {
        final var loader = new FXMLLoader(getClass().getResource("/ui/main-view.fxml"));

        final ControllerFactory factory = this::createController;
        loader.setControllerFactory(factory);

        final var scene = new Scene(loader.load(), 1200, 800);
        stage.setTitle("JBolt");
        stage.setScene(scene);
        stage.show();
    }

    private Object createController(final Class<?> type) {
        if (type == MainController.class) {
            return new MainController(this::createController, uiMessageService);
        }

        if (type == RequestTabController.class) {
            final var vm = new RequestTabViewModel(requestExecutionService);
            return new RequestTabController(vm);
        }

        throw new IllegalArgumentException("Unknown controller type requested: " + type.getName());
    }

    public static void main(final String[] args) {
        launch(args);
    }
}