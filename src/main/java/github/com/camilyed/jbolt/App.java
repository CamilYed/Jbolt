package github.com.camilyed.jbolt;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final String HELLO_VIEW_FXML = "/github/com/camilyed/jbolt/hello-view.fxml";

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        var fxmlLoader = new FXMLLoader(App.class.getResource(HELLO_VIEW_FXML));
        var scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("JBolt | API Tool");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}