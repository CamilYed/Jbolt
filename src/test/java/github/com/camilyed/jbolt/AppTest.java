package github.com.camilyed.jbolt;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testfx.assertions.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class AppTest {

    @Start
    public void start(Stage stage) throws Exception {
        // Launch the application using the full package path for the App class
        new App().start(stage);
    }


    @Test
    void testApiRequestFlow(FxRobot robot) throws TimeoutException {
        // Wait for the UI to be fully initialized and rendered
        WaitForAsyncUtils.waitForFxEvents();

        // 1. Locate the URL field and set the text within the JavaFX Application Thread
        // We use the #urlField ID defined in request-tab.fxml
        TextField urlField = robot.lookup("#urlField").queryAs(TextField.class);
        robot.interact(() -> {
            urlField.requestFocus();
            urlField.setText("https://httpbin.org/get");
        });

        // Verify that the text was correctly injected into the field
        assertThat(urlField.getText()).isEqualTo("https://httpbin.org/get");

        // 2. Locate the SEND button by its style class and text content
        Button sendBtn = robot.lookup(".button").queryAllAs(Button.class).stream()
                .filter(b -> "SEND".equals(b.getText()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("SEND button not found in the current scene"));

        // Use the fire() method to trigger the button action programmatically.
        // This is a robust workaround for mouse event issues in nested TabPanes or CI environments.
        robot.interact(() -> {
            sendBtn.requestFocus();
            sendBtn.fire();
        });

        // 3. Wait for the response to appear in the TextArea (max 15 seconds)
        // We look for specific markers indicating a successful round-trip to httpbin
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, () -> {
            TextArea responseArea = robot.lookup("#responseArea").queryAs(TextArea.class);
            String content = responseArea.getText();
            return content.contains("200") || content.contains("https://httpbin.org/get");
        });

        // Final assertion to verify the response content
        TextArea responseArea = robot.lookup("#responseArea").queryAs(TextArea.class);
        assertThat(responseArea.getText()).contains("https://httpbin.org/get");
    }
}