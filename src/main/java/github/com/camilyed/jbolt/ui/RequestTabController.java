package github.com.camilyed.jbolt.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.fasterxml.jackson.databind.JsonNode;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.ui.model.RequestTabViewModel;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Builds a single request tab: the method/URL/send bar, a request-body/headers editor, and a
 * response panel. Purely a view - all state and behavior lives in the injected {@link
 * RequestTabViewModel}, which this class binds its controls to.
 */
public final class RequestTabController implements Component<VBox> {

  // Postman-style method colors. These are plain JavaFX Color literals rather than AtlantaFX
  // Styles classes because a CSS class applied to the ComboBox root isn't guaranteed to reach
  // deep enough into its skin to tint the box itself - a small colored dot next to the method
  // name, drawn directly, renders identically regardless of the control's internal structure.
  private static final Color GET_COLOR = Color.web("#3fb950");
  private static final Color POST_COLOR = Color.web("#58a6ff");
  private static final Color PUT_PATCH_COLOR = Color.web("#d29922");
  private static final Color DELETE_COLOR = Color.web("#f85149");
  private static final Color OTHER_METHOD_COLOR = Color.web("#8b949e");

  // Colors shared by the response JSON tree and the raw highlighted view.
  private static final Color JSON_KEY_COLOR = Color.web("#79c0ff");
  private static final Color JSON_STRING_COLOR = Color.web("#a5d6ff");
  private static final Color JSON_SCALAR_COLOR = Color.web("#d2a8ff");
  private static final Color JSON_CONTAINER_COLOR = Color.web("#8b949e");
  // Hover color for the raw view's fold toggles - the same accent blue used for POST, reused here
  // as the app's one "this is interactive" signal rather than inventing a second one.
  private static final Color FOLD_TOGGLE_HOVER_COLOR = Color.web("#58a6ff");
  private static final String FOLD_TOGGLE_STYLE = "-fx-font-size: 1.25em; -fx-font-weight: bold;";

  private static final int INDENT_GUIDE_WIDTH = 14;
  private static final String INDENT_UNIT = "  ";

  private ComboBox<HttpMethod> methodCombo;
  private TextField urlField;
  private TextArea requestBodyArea;
  private TextArea responseArea;
  private TreeView<JsonRow> responseTree;
  private TextFlow rawJsonFlow;
  private ScrollPane rawJsonScroll;
  private HBox viewToggleBox;
  private ToggleButton treeToggleBtn;
  private ToggleButton rawToggleBtn;
  private Label statusLabel;
  private Label timeLabel;
  private Button sendBtn;

  // Whether the last response parsed as a JSON object/array - the only case where a tree or a
  // highlighted raw view makes sense. Read by updateVisibleView() so the toggle listener can
  // recompute visibility without needing the JsonNode itself.
  private boolean hasContainerJson;
  private boolean rawModeSelected;

  // Which containers are collapsed in the raw view, keyed by node identity, and the JsonNode the
  // raw view was last built from - both reset whenever a new response arrives, since a stale
  // JsonNode as a map key would otherwise leak every past response's tree for the tab's lifetime.
  private final Map<JsonNode, Boolean> rawFoldedNodes = new IdentityHashMap<>();
  private JsonNode currentJson;
  private int foldIdSeq;

  private final RequestTabViewModel vm;

  public RequestTabController(final RequestTabViewModel vm) {
    this.vm = vm;
  }

  @Override
  public VBox build() {
    final var requestBar = buildRequestBar();
    final var mainSplit = buildMainSplit();
    VBox.setVgrow(mainSplit, Priority.ALWAYS);

    final var root = new VBox(12, requestBar, mainSplit);
    root.setPadding(new Insets(12));

    setupBindings();
    return root;
  }

  private HBox buildRequestBar() {
    methodCombo = new ComboBox<>();
    methodCombo.setId("methodCombo");
    methodCombo.setPrefWidth(130);
    methodCombo.getStyleClass().add(Styles.ROUNDED);
    methodCombo.setButtonCell(methodCell());
    methodCombo.setCellFactory(_ -> methodCell());

    urlField = new TextField();
    urlField.setId("urlField");
    urlField.setPromptText("https://api.example.com/resource");
    urlField.getStyleClass().add(Styles.ROUNDED);
    HBox.setHgrow(urlField, Priority.ALWAYS);

    sendBtn = new Button("SEND");
    sendBtn.setId("sendBtn");
    sendBtn.getStyleClass().addAll(Styles.ACCENT, Styles.ROUNDED);
    sendBtn.setOnAction(event -> onSend());

    final var bar = new HBox(10, methodCombo, urlField, sendBtn);
    bar.setAlignment(Pos.CENTER_LEFT);
    return bar;
  }

  private SplitPane buildMainSplit() {
    final var split = new SplitPane(buildRequestEditor(), buildResponseCard());
    split.setOrientation(Orientation.VERTICAL);
    split.setDividerPositions(0.45);
    return split;
  }

  private TabPane buildRequestEditor() {
    requestBodyArea = new TextArea();
    requestBodyArea.setId("requestBodyArea");
    requestBodyArea.setPromptText("Request body (JSON)…");
    requestBodyArea.getStyleClass().add(Styles.TEXT_SMALL);
    final var bodyTab = new Tab("Body", requestBodyArea);
    bodyTab.setClosable(false);

    final var headersPlaceholder = new Label("Headers editor coming soon");
    headersPlaceholder.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
    final var headersPane = new VBox(8, headersPlaceholder);
    headersPane.setPadding(new Insets(16));
    final var headersTab = new Tab("Headers", headersPane);
    headersTab.setClosable(false);

    final var tabs = new TabPane(bodyTab, headersTab);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.getStyleClass().addAll(Styles.TABS_CLASSIC, Tweaks.EDGE_TO_EDGE);
    return tabs;
  }

  private Region buildResponseCard() {
    final var title = new Label("Response");
    title.getStyleClass().addAll(Styles.TITLE_4, Styles.TEXT_MUTED);

    statusLabel = new Label("—");
    statusLabel.setId("statusLabel");
    statusLabel.getStyleClass().add(Styles.TEXT_BOLD);
    timeLabel = new Label("— ms");
    timeLabel.setId("timeLabel");
    timeLabel.getStyleClass().add(Styles.TEXT_MUTED);

    final var spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    final var statusRow =
        new HBox(
            10,
            statusLabel,
            new Separator(Orientation.VERTICAL),
            timeLabel,
            spacer,
            buildViewToggle());
    statusRow.setAlignment(Pos.CENTER_LEFT);

    final var card = new Card();
    card.setHeader(title);
    card.setSubHeader(statusRow);
    card.setBody(buildResponseBody());
    card.getStyleClass().add(Styles.ELEVATED_1);
    card.setMaxHeight(Double.MAX_VALUE);
    card.setMaxWidth(Double.MAX_VALUE);
    // ELEVATED_1's shadow alone reads as barely-there on PrimerDark, so give the card an explicit,
    // visible edge using the same AtlantaFX theme token already confirmed to render correctly for
    // the sidebar separator.
    card.setStyle(
        "-fx-border-color: -color-border-default; -fx-border-width: 1; "
            + "-fx-border-radius: 8; -fx-background-radius: 8;");
    return card;
  }

  /**
   * A small "Tree | Raw" segmented control that picks how a JSON response is displayed. Hidden
   * whenever the response isn't a JSON object/array, since neither view applies to plain text or
   * scalar bodies.
   */
  private HBox buildViewToggle() {
    treeToggleBtn = new ToggleButton("Tree");
    treeToggleBtn.setId("treeToggleBtn");
    treeToggleBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL, Styles.LEFT_PILL);

    rawToggleBtn = new ToggleButton("Raw");
    rawToggleBtn.setId("rawToggleBtn");
    rawToggleBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL, Styles.RIGHT_PILL);

    final var group = new ToggleGroup();
    treeToggleBtn.setToggleGroup(group);
    rawToggleBtn.setToggleGroup(group);
    treeToggleBtn.setSelected(true);

    group
        .selectedToggleProperty()
        .addListener(
            (_, oldToggle, newToggle) -> {
              if (newToggle == null) {
                // A ToggleGroup allows clicking the active toggle to deselect it; a view always
                // needs exactly one mode selected, so put the old one straight back.
                oldToggle.setSelected(true);
                return;
              }
              rawModeSelected = newToggle == rawToggleBtn;
              updateVisibleView();
            });

    viewToggleBox = new HBox(6, treeToggleBtn, rawToggleBtn);
    viewToggleBox.setId("viewToggleBox");
    viewToggleBox.setAlignment(Pos.CENTER_RIGHT);
    viewToggleBox.setVisible(false);
    viewToggleBox.setManaged(false);
    return viewToggleBox;
  }

  /**
   * The card body is one of three views sharing a {@link StackPane}: the plain text area (initial
   * empty state, and non-JSON or scalar bodies), the collapsible tree, or the highlighted raw
   * JSON text - {@link #updateVisibleView()} toggles which one is visible.
   */
  private StackPane buildResponseBody() {
    responseArea = new TextArea();
    responseArea.setId("responseArea");
    responseArea.setEditable(false);
    responseArea.setPromptText("Response will appear here…");
    responseArea.getStyleClass().add(Styles.TEXT_SMALL);
    responseArea.setMaxHeight(Double.MAX_VALUE);
    responseArea.setMaxWidth(Double.MAX_VALUE);

    responseTree = new TreeView<>();
    responseTree.setId("responseTree");
    responseTree.setShowRoot(false);
    responseTree.getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.TEXT_SMALL);
    responseTree.setCellFactory(_ -> jsonCell());
    responseTree.setMaxHeight(Double.MAX_VALUE);
    responseTree.setMaxWidth(Double.MAX_VALUE);

    rawJsonFlow = new TextFlow();
    rawJsonFlow.setId("rawJsonFlow");
    rawJsonFlow.getStyleClass().add(Styles.TEXT_SMALL);
    rawJsonFlow.setStyle("-fx-font-family: 'Menlo', 'Consolas', monospace;");

    rawJsonScroll = new ScrollPane(rawJsonFlow);
    rawJsonScroll.setId("rawJsonView");
    rawJsonScroll.setFitToWidth(true);
    rawJsonScroll.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
    rawJsonScroll.setMaxHeight(Double.MAX_VALUE);
    rawJsonScroll.setMaxWidth(Double.MAX_VALUE);

    final var stack = new StackPane(responseArea, responseTree, rawJsonScroll);
    VBox.setVgrow(stack, Priority.ALWAYS);
    return stack;
  }

  /**
   * A tree cell rendering a {@link JsonRow} as "key: value", colored by JSON value type, preceded
   * by a thin vertical guide line per ancestor level beyond the top one so a deeply nested group
   * stays visually traceable back to where it opened.
   */
  private TreeCell<JsonRow> jsonCell() {
    return new TreeCell<>() {
      @Override
      protected void updateItem(final JsonRow row, final boolean empty) {
        super.updateItem(row, empty);
        if (empty || row == null) {
          setGraphic(null);
          setText(null);
          return;
        }
        final var key = new Text(row.key() + ": ");
        key.setFill(JSON_KEY_COLOR);
        final var value = new Text(row.valuePreview());
        value.setFill(jsonValueColor(row));
        final var content = new TextFlow(key, value);

        final var level =
            getTreeView() == null ? 0 : getTreeView().getTreeItemLevel(getTreeItem());
        final var line = new HBox(buildIndentGuides(level), content);
        line.setAlignment(Pos.CENTER_LEFT);
        setGraphic(line);
        setText(null);
      }
    };
  }

  /**
   * One thin vertical line per ancestor level beyond the first - the root's direct children (the
   * top-level JSON fields) already read as a flat, un-nested list thanks to the tree's own
   * indentation, so they get none.
   */
  private Region buildIndentGuides(final int level) {
    final var guides = new HBox();
    for (var i = 1; i < level; i++) {
      final var guide = new Region();
      guide.setPrefWidth(INDENT_GUIDE_WIDTH);
      guide.setMinWidth(INDENT_GUIDE_WIDTH);
      guide.setStyle(
          "-fx-border-color: transparent transparent transparent -color-border-default; "
              + "-fx-border-width: 0 0 0 1;");
      guides.getChildren().add(guide);
    }
    return guides;
  }

  private Color jsonValueColor(final JsonRow row) {
    if (row.isContainer()) {
      return JSON_CONTAINER_COLOR;
    }
    if (row.node().isTextual()) {
      return JSON_STRING_COLOR;
    }
    return JSON_SCALAR_COLOR;
  }

  /** Rebuilds the tree and the raw view for a new response, then shows whichever is selected. */
  private void updateResponseViews(final JsonNode json) {
    hasContainerJson = json != null && (json.isObject() || json.isArray());
    currentJson = json;
    rawFoldedNodes.clear();
    responseTree.setRoot(hasContainerJson ? JsonTreeBuilder.build("root", json) : null);
    refreshRawView();
    viewToggleBox.setVisible(hasContainerJson);
    viewToggleBox.setManaged(hasContainerJson);
    updateVisibleView();
  }

  /** Re-renders the raw view from {@link #currentJson} and the current fold state. */
  private void refreshRawView() {
    rawJsonFlow.getChildren().setAll(hasContainerJson ? buildRawJsonNodes(currentJson) : List.of());
  }

  private void updateVisibleView() {
    final var showTree = hasContainerJson && !rawModeSelected;
    final var showRaw = hasContainerJson && rawModeSelected;
    responseTree.setVisible(showTree);
    responseTree.setManaged(showTree);
    rawJsonScroll.setVisible(showRaw);
    rawJsonScroll.setManaged(showRaw);
    responseArea.setVisible(!hasContainerJson);
    responseArea.setManaged(!hasContainerJson);
  }

  /**
   * Renders a JSON value as pretty-printed, colored {@link Text} runs - punctuation (braces,
   * brackets, colons, commas) is muted so it reads as structure rather than content, while keys
   * and values keep the same palette as the tree. This reproduces the response exactly as the API
   * sent it, unlike the tree's per-row size previews, for anyone who wants to see the whole
   * document at once. Every object/array's opening bracket is itself a clickable {@link
   * #foldToggle(JsonNode, String)} that collapses it to a "{…}"/"[…]" placeholder, the same
   * gesture a code editor's gutter fold icon offers, without needing a gutter here.
   */
  private List<Text> buildRawJsonNodes(final JsonNode json) {
    foldIdSeq = 0;
    final var out = new ArrayList<Text>();
    appendJson(out, json, 0);
    return out;
  }

  private void appendJson(final List<Text> out, final JsonNode node, final int depth) {
    if (node.isObject()) {
      final var fields = new ArrayList<Map.Entry<String, JsonNode>>();
      node.fields().forEachRemaining(fields::add);
      if (fields.isEmpty()) {
        appendPunct(out, "{}");
        return;
      }
      out.add(foldToggle(node, "{"));
      if (isFolded(node)) {
        appendPunct(out, "…}");
        return;
      }
      appendPlain(out, "\n");
      for (var i = 0; i < fields.size(); i++) {
        final var entry = fields.get(i);
        appendPlain(out, indent(depth + 1));
        appendColored(out, "\"" + entry.getKey() + "\"", JSON_KEY_COLOR);
        appendPunct(out, ": ");
        appendJson(out, entry.getValue(), depth + 1);
        appendPunct(out, i < fields.size() - 1 ? ",\n" : "\n");
      }
      appendPlain(out, indent(depth));
      appendPunct(out, "}");
    } else if (node.isArray()) {
      if (node.size() == 0) {
        appendPunct(out, "[]");
        return;
      }
      out.add(foldToggle(node, "["));
      if (isFolded(node)) {
        appendPunct(out, "…]");
        return;
      }
      appendPlain(out, "\n");
      for (var i = 0; i < node.size(); i++) {
        appendPlain(out, indent(depth + 1));
        appendJson(out, node.get(i), depth + 1);
        appendPunct(out, i < node.size() - 1 ? ",\n" : "\n");
      }
      appendPlain(out, indent(depth));
      appendPunct(out, "]");
    } else if (node.isTextual()) {
      appendColored(out, "\"" + node.asText() + "\"", JSON_STRING_COLOR);
    } else if (node.isNull()) {
      appendColored(out, "null", JSON_SCALAR_COLOR);
    } else {
      appendColored(out, node.asText(), JSON_SCALAR_COLOR);
    }
  }

  private boolean isFolded(final JsonNode node) {
    return rawFoldedNodes.getOrDefault(node, Boolean.FALSE);
  }

  /**
   * A clickable "{" or "[" that toggles its own container's fold state and re-renders the raw
   * view. Rendered noticeably larger and bolder than the surrounding punctuation - at normal text
   * size a lone bracket reads as inert, too small to comfortably aim at and easy to mistake for
   * plain structure rather than a control - and it swaps to the accent color with an underline on
   * hover so the pointer confirms it's interactive before the click even lands. IDs are assigned
   * in traversal order ("fold-0", "fold-1", …) purely so tests can target a specific container
   * without depending on JSON key names, which may contain characters a CSS id selector can't
   * express.
   */
  private Text foldToggle(final JsonNode node, final String openChar) {
    final var text = new Text(openChar);
    text.setId("fold-" + foldIdSeq++);
    text.setFill(JSON_CONTAINER_COLOR);
    text.setStyle(FOLD_TOGGLE_STYLE);
    text.setCursor(Cursor.HAND);
    text.setOnMouseEntered(
        _ -> {
          text.setFill(FOLD_TOGGLE_HOVER_COLOR);
          text.setUnderline(true);
        });
    text.setOnMouseExited(
        _ -> {
          text.setFill(JSON_CONTAINER_COLOR);
          text.setUnderline(false);
        });
    text.setOnMouseClicked(
        _ -> {
          rawFoldedNodes.put(node, !isFolded(node));
          refreshRawView();
        });
    return text;
  }

  private static String indent(final int depth) {
    return INDENT_UNIT.repeat(depth);
  }

  private void appendPlain(final List<Text> out, final String text) {
    out.add(new Text(text));
  }

  private void appendPunct(final List<Text> out, final String text) {
    final var node = new Text(text);
    node.setFill(JSON_CONTAINER_COLOR);
    out.add(node);
  }

  private void appendColored(final List<Text> out, final String text, final Color color) {
    final var node = new Text(text);
    node.setFill(color);
    out.add(node);
  }

  private void setupBindings() {
    methodCombo.setItems(vm.methods);
    methodCombo.valueProperty().bindBidirectional(vm.method);

    urlField.textProperty().bindBidirectional(vm.url);
    requestBodyArea.textProperty().bindBidirectional(vm.requestBody);

    responseArea.textProperty().bind(vm.responseBody);
    statusLabel.textProperty().bind(vm.statusText);
    timeLabel.textProperty().bind(vm.timeText);

    vm.responseJson.addListener((_, _, json) -> updateResponseViews(json));
    updateResponseViews(vm.responseJson.get());

    vm.statusClass.addListener(
        (_, oldClass, newClass) -> {
          if (oldClass != null && !oldClass.isEmpty()) {
            statusLabel.getStyleClass().remove(oldClass);
          }
          if (newClass != null && !newClass.isEmpty()) {
            statusLabel.getStyleClass().add(newClass);
          }
        });

    sendBtn.disableProperty().bind(vm.loading.or(vm.url.isEmpty()));
  }

  /**
   * A list cell showing the method name next to a colored dot - used both for the combo's own
   * button face and for each row in its dropdown, so the selected method reads at a glance.
   */
  private ListCell<HttpMethod> methodCell() {
    return new ListCell<>() {
      private final Circle dot = new Circle(4);

      {
        setGraphic(dot);
        setGraphicTextGap(8);
        getStyleClass().add(Styles.TEXT_BOLD);
      }

      @Override
      protected void updateItem(final HttpMethod item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
        } else {
          setText(item.name());
          dot.setFill(methodColor(item));
        }
      }
    };
  }

  private Color methodColor(final HttpMethod method) {
    return switch (method) {
      case GET -> GET_COLOR;
      case POST -> POST_COLOR;
      case PUT, PATCH -> PUT_PATCH_COLOR;
      case DELETE -> DELETE_COLOR;
      case HEAD, OPTIONS -> OTHER_METHOD_COLOR;
    };
  }

  private void onSend() {
    vm.sendRequest();
  }
}
