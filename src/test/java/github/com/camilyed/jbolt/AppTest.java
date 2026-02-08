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

import static org.testfx.assertions.api.Assertions.assertThat;


@ExtendWith(ApplicationExtension.class)
class AppTest {

    @Start
    public void start(Stage stage) throws Exception {
        new App().start(stage);
    }

    @Test
    void testApiRequestFlow(FxRobot robot) {
        // given
        Button sendBtn = robot.lookup("#sendBtn").queryButton();
        assertThat(sendBtn).hasText("SEND");

        // when
        robot.clickOn("#urlField");
        robot.interact(() -> {
            TextField urlField = robot.lookup("#urlField").queryAs(TextField.class);
            urlField.setText("https://httpbin.org/get");
        });

        // and
        TextField urlField = robot.lookup("#urlField").queryAs(TextField.class);
        assertThat(urlField.getText()).isEqualTo("https://httpbin.org/get");

        // when
        robot.clickOn(sendBtn);
        robot.interact(sendBtn::fire);

        // then
        WaitForAsyncUtils.waitForFxEvents();


        // and
        TextArea responseArea = robot.lookup("#responseArea").queryAs(TextArea.class);
        assertThat(responseArea.getText())
                .contains("Sending GET to: https://httpbin.org/get");
    }
}