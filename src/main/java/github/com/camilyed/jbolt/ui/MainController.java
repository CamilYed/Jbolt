package github.com.camilyed.jbolt.ui;

import atlantafx.base.theme.PrimerDark;
import github.com.camilyed.jbolt.common.result.Result;
import github.com.camilyed.jbolt.ui.model.UiError;
import github.com.camilyed.jbolt.ui.service.UiMessageService;
import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Main window controller managing the tab-based workspace.
 */
public final class MainController {

    private static final String REQUEST_TAB_FXML = "/ui/request-tab.fxml";

    @FXML private TabPane requestTabs;

    private final ControllerFactory controllerFactory;
    private final UiMessageService uiMessageService;

    public MainController(final ControllerFactory controllerFactory, final UiMessageService uiMessageService) {
        this.controllerFactory = controllerFactory;
        this.uiMessageService = uiMessageService;
    }

    @FXML
    public void initialize() {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        openNewTab();
    }

    void openNewTab() {
        loadTabContent(REQUEST_TAB_FXML)
                .onSuccess(this::addNewTabToPane)
                .onFailure(this::handleLoadingError);
    }

    private Result<Parent> loadTabContent(final String fxmlPath) {
        return Result.of(() -> {
            final var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new java.io.IOException("FXML resource not found: " + fxmlPath);
            }
            final var loader = new FXMLLoader(resource);
            loader.setControllerFactory(controllerFactory);
            return (Parent) loader.load();
        });
    }

    private void addNewTabToPane(final Parent root) {
        final var tab = new Tab("New Request");
        tab.setContent(root);
        tab.setClosable(true);

        final var tabs = requestTabs.getTabs();
        final var lastIndex = tabs.size() - 1;

        tabs.add(Math.max(lastIndex, 0), tab);
        requestTabs.getSelectionModel().select(tab);
    }

    private void handleLoadingError(final Throwable error) {
        uiMessageService.showError(new UiError(
                "View Loading Failed",
                "Could not load the request tab component.",
                "UI-LOAD-001",
                error
        ));
    }

    @FXML
    private void handleAddTabTabSelected(final Event event) {
        final var tab = (Tab) event.getSource();
        if (tab.isSelected()) {
            openNewTab();
        }
    }
}