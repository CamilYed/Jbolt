package github.com.camilyed.jbolt;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final String MAIN_VIEW_FXML = "/ui/main-view.fxml";
    private static final String APP_TITLE = "JBolt | API Tool";

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        final var fxmlLoader = new FXMLLoader(App.class.getResource(MAIN_VIEW_FXML));
        final var scene = new Scene(fxmlLoader.load());
        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.setMinHeight(600);
        stage.setMinWidth(900);
        stage.centerOnScreen();
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}