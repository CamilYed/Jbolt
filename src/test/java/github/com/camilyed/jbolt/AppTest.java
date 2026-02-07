package github.com.camilyed.jbolt;

import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.testfx.assertions.api.Assertions.assertThat;


@ExtendWith(ApplicationExtension.class)
class AppTest {

    @Start
    private void start(Stage stage) throws Exception {
        new App().start(stage);
    }

    @Test
    void testButtonClick(FxRobot robot) {
        robot.clickOn("#actionBtn");
        assertThat(robot.lookup("#welcomeText").queryLabeled())
                .hasText("Welcome to JBolt");
    }
}