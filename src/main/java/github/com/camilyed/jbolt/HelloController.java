package github.com.camilyed.jbolt;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("JBolt Engine: Status OK (JDK 25)");
    }
}