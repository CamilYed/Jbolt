package github.com.camilyed.jbolt.ui;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Main window controller responsible for managing the TabPane and application-wide layout.
 */
public final class MainController {

    @FXML private TabPane requestTabs;

    @FXML
    public void initialize() {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        openNewTab();
    }

    private void openNewTab() {
        try {
            final var loader = new FXMLLoader(getClass().getResource("/ui/request-tab.fxml"));
            final var tab = new Tab("New Request");

            tab.setContent(loader.load());
            tab.setClosable(true);

            final var tabs = requestTabs.getTabs();
            final var lastIndex = tabs.size() - 1;

            tabs.add(Math.max(lastIndex, 0), tab);
            requestTabs.getSelectionModel().select(tab);

        } catch (final Exception e) {
            throw new RuntimeException("Failed to open request tab", e);
        }
    }

    @FXML
    private void handleAddTabTabSelected(final Event event) {
        final var tab = (Tab) event.getSource();
        if (tab.isSelected()) {
            openNewTab();
        }
    }
}