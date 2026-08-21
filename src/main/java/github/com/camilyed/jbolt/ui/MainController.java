package github.com.camilyed.jbolt.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import github.com.camilyed.jbolt.ui.model.UiError;
import github.com.camilyed.jbolt.ui.service.UiMessageService;
import github.com.camilyed.jbolt.ui.service.ViewLoader;
import javafx.application.Application;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Builds and drives the application's main window: a collections sidebar on the left and a pane of
 * request tabs in the center, each opened on demand via {@link #openNewTab()}.
 */
public final class MainController implements Component<BorderPane> {

  private static final String REQUEST_TAB_VIEW = "request-tab";
  private static final double SIDEBAR_WIDTH = 260;

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
    viewLoader
        .load(REQUEST_TAB_VIEW)
        .onSuccess(this::addNewTabToPane)
        .onFailure(this::handleLoadingError);
  }

  private VBox buildCollectionsPane() {
    final var pane = new VBox(buildCollectionsHeader(), buildCollectionTree());
    pane.setId("collectionsPane");
    pane.setPrefWidth(SIDEBAR_WIDTH);
    pane.setMinWidth(SIDEBAR_WIDTH);
    pane.getStyleClass().add(Styles.BG_SUBTLE);
    // A thin separator between the sidebar and the request area, drawn with AtlantaFX's own
    // theme color token so it stays correct for both light and dark variants.
    pane.setStyle("-fx-border-color: -color-border-default; -fx-border-width: 0 1 0 0;");
    return pane;
  }

  private HBox buildCollectionsHeader() {
    final var title = new Label("COLLECTIONS");
    title.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_MUTED);

    final var spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    final var newCollectionBtn = new Button("+");
    newCollectionBtn.setId("newCollectionBtn");
    newCollectionBtn.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.ACCENT, Styles.SMALL);
    // Collection creation isn't wired up yet (tracked in ROADMAP.md Phase 1) - disable rather
    // than ship a button that silently does nothing when clicked.
    newCollectionBtn.setDisable(true);
    newCollectionBtn.setTooltip(new Tooltip("Coming soon"));

    final var header = new HBox(8, title, spacer, newCollectionBtn);
    header.setAlignment(Pos.CENTER_LEFT);
    header.setPadding(new Insets(14, 12, 10, 16));
    return header;
  }

  private StackPane buildCollectionTree() {
    final var collectionTree = new TreeView<Object>();
    collectionTree.setId("collectionTree");
    collectionTree.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
    collectionTree.setShowRoot(false);

    // TreeView has no built-in "placeholder" (that's only on ListView/TableView), so the empty
    // state is a label stacked on top, shown only while no root has been set yet.
    final var emptyState = new Label("No collections yet");
    emptyState.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
    emptyState.setMouseTransparent(true);
    emptyState.visibleProperty().bind(collectionTree.rootProperty().isNull());

    final var stack = new StackPane(collectionTree, emptyState);
    VBox.setVgrow(stack, Priority.ALWAYS);
    return stack;
  }

  private TabPane buildRequestTabs() {
    requestTabs = new TabPane();
    requestTabs.setId("requestTabs");
    requestTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    requestTabs.getStyleClass().addAll(Styles.TABS_FLOATING, Tweaks.EDGE_TO_EDGE);

    final var addTab = new Tab("+");
    addTab.setId("addTabBtn");
    addTab.setClosable(false);
    // Adding the first (only) tab to an empty TabPane auto-selects it, firing
    // onSelectionChanged synchronously. Wire the handler AFTER adding the tab so that initial,
    // automatic selection doesn't itself trigger openNewTab() - only a real, later selection
    // (the user actually clicking "+") should.
    requestTabs.getTabs().add(addTab);
    addTab.setOnSelectionChanged(this::handleAddTabTabSelected);

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
    uiMessageService.showError(
        new UiError(
            "View Loading Failed",
            "Could not load the request tab component.",
            "UI-LOAD-001",
            error));
  }

  void handleAddTabTabSelected(final Event event) {
    final var tab = (Tab) event.getSource();
    if (tab.isSelected()) {
      openNewTab();
    }
  }
}
