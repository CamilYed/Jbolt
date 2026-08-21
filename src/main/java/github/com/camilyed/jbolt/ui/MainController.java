package github.com.camilyed.jbolt.ui;

import atlantafx.base.theme.PrimerDark;
import github.com.camilyed.jbolt.ui.model.UiError;
import github.com.camilyed.jbolt.ui.service.UiMessageService;
import github.com.camilyed.jbolt.ui.service.ViewLoader;
import javafx.application.Application;
import javafx.event.Event;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Builds and drives the application's main window: a collections sidebar on the left and a
 * pane of request tabs in the center, each opened on demand via {@link #openNewTab()}.
 */
public final class MainController implements Component<BorderPane> {

    private static final String REQUEST_TAB_VIEW = "request-tab";

    TabPane requestTabs;

    private final ViewLoader viewLoader;
    private final UiMessageService uiMessageService;

    public MainController(final ViewLoader viewLoader, final UiMessageService uiMessageService) {
        this.viewLoader = viewLoader;
        this.uiMessageService = uiMessageService;
    }

    @Override
    public BorderPane build() {
        final var root = new BorderPane();
        root.setLeft(buildCollectionsPane());
        root.setCenter(buildRequestTabs());
        return root;
    }

    /** Applies the application theme and opens the first request tab. Call after {@link #build()}. */
    public void initialize() {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        openNewTab();
    }

    void openNewTab() {
        viewLoader.load(REQUEST_TAB_VIEW)
                .onSuccess(this::addNewTabToPane)
                .onFailure(this::handleLoadingError);
    }

    private VBox buildCollectionsPane() {
        final var label = new Label("COLLECTIONS");
        label.getStyleClass().addAll("title-4", "text-muted");

        final var collectionTree = new TreeView<Object>();
        collectionTree.setId("collectionTree");
        collectionTree.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(collectionTree, Priority.ALWAYS);

        final var pane = new VBox(label, collectionTree);
        pane.setId("collectionsPane");
        pane.setPrefWidth(260);
        pane.getStyleClass().addAll("content-pane", "dense", "border-right");
        return pane;
    }

    private TabPane buildRequestTabs() {
        requestTabs = new TabPane();
        requestTabs.setId("requestTabs");
        requestTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        requestTabs.getStyleClass().add("floating");

        final var addTab = new Tab("+");
        addTab.setId("addTabBtn");
        addTab.setClosable(false);
        addTab.setOnSelectionChanged(this::handleAddTabTabSelected);
        requestTabs.getTabs().add(addTab);

        return requestTabs;
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

    void handleAddTabTabSelected(final Event event) {
        final var tab = (Tab) event.getSource();
        if (tab.isSelected()) {
            openNewTab();
        }
    }
}
