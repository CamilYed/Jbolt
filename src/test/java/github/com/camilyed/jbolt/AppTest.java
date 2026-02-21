package github.com.camilyed.jbolt;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
    public void start(final Stage stage) throws Exception {
        final var app = new App();
        app.init();
        app.start(stage);
    }

    @Test
    void testApiRequestFlow(final FxRobot robot) throws TimeoutException {
        // given
        WaitForAsyncUtils.waitForFxEvents();
        final var urlField = robot.lookup("#urlField").queryAs(TextField.class);

        // when
        robot.interact(() -> {
            urlField.requestFocus();
            urlField.setText("https://httpbin.org/get");
        });

        // then
        assertThat(urlField.getText()).isEqualTo("https://httpbin.org/get");

        // and
        final var sendBtn = robot.lookup(".button").queryAllAs(Button.class).stream()
                .filter(b -> "SEND".equals(b.getText()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("SEND button not found in the current scene"));

        // when
        robot.interact(() -> {
            sendBtn.requestFocus();
            sendBtn.fire();
        });

        // then
        WaitForAsyncUtils.waitFor(15, TimeUnit.SECONDS, () -> {
            final var responseArea = robot.lookup("#responseArea").queryAs(TextArea.class);
            final var content = responseArea.getText();
            return content.contains("200") || content.contains("https://httpbin.org/get");
        });

        final var responseArea = robot.lookup("#responseArea").queryAs(TextArea.class);
        assertThat(responseArea.getText()).contains("https://httpbin.org/get");
    }
}