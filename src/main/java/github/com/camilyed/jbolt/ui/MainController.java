package github.com.camilyed.jbolt.ui;

import atlantafx.base.theme.PrimerDark;
import github.com.camilyed.jbolt.ui.model.UiError;
import github.com.camilyed.jbolt.ui.service.UiMessageService;
import github.com.camilyed.jbolt.ui.service.ViewLoader;
import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public final class MainController {

    private static final String REQUEST_TAB_FXML = "/ui/request-tab.fxml";

    @FXML
    TabPane requestTabs;

    private final ViewLoader viewLoader;
    private final UiMessageService uiMessageService;

    public MainController(
            final ViewLoader viewLoader,
            final UiMessageService uiMessageService) {
        this.viewLoader = viewLoader;
        this.uiMessageService = uiMessageService;
    }

    @FXML
    public void initialize() {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        openNewTab();
    }

    void openNewTab() {
        viewLoader.load(REQUEST_TAB_FXML)
                .onSuccess(this::addNewTabToPane)
                .onFailure(this::handleLoadingError);
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
    void handleAddTabTabSelected(final Event event) {
        final var tab = (Tab) event.getSource();
        if (tab.isSelected()) {
            openNewTab();
        }
    }
}