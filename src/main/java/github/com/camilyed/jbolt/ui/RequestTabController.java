package github.com.camilyed.jbolt.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.fasterxml.jackson.databind.JsonNode;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.ui.model.KeyValueRow;
import github.com.camilyed.jbolt.ui.model.RequestTabViewModel;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ObservableList;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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

  /** Which of the three response views currently applies, decided by what the body parsed as. */
  private enum BodyKind {
    JSON,
    XML,
    NONE
  }

  // What kind of body the last response was - drives both which view is visible and whether the
  // Tree/Raw toggle even makes sense (XML has no tree yet, so it's raw-only). Read by
  // updateVisibleView() so the toggle listener can recompute visibility without re-deriving it.
  private BodyKind bodyKind = BodyKind.NONE;
  // Raw is the default for both JSON and XML - it shows the response exactly as the server sent
  // it, which reads better on first glance than a tree the user has to already trust before
  // expanding. Initialized to match rawToggleBtn's default selection in buildViewToggle().
  private boolean rawModeSelected = true;

  // Which containers are collapsed in the raw view, keyed by node identity, and the JsonNode the
  // raw view was last built from - both reset whenever a new response arrives, since a stale
  // JsonNode as a map key would otherwise leak every past response's tree for the tab's lifetime.
  private final Map<JsonNode, Boolean> rawFoldedNodes = new IdentityHashMap<>();
  private JsonNode currentJson;
  // Same idea as rawFoldedNodes/currentJson, but for XML elements - org.w3c.dom nodes don't
  // override equals()/hashCode(), so identity semantics fall out of a plain IdentityHashMap the
  // same way they do for JsonNode.
  private final Map<Element, Boolean> xmlFoldedNodes = new IdentityHashMap<>();
  private Document currentXmlDoc;
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

    final var paramsTab = new Tab("Params", buildKeyValueEditor(vm.queryParams, "queryParams"));
    paramsTab.setClosable(false);

    final var headersTab = new Tab("Headers", buildKeyValueEditor(vm.headers, "headers"));
    headersTab.setClosable(false);

    final var tabs = new TabPane(bodyTab, paramsTab, headersTab);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.getStyleClass().addAll(Styles.TABS_CLASSIC, Tweaks.EDGE_TO_EDGE);
    return tabs;
  }

  /**
   * A key/value table plus an "+ Add" button beneath it - shared by the Headers and Params tabs,
   * since both edit the exact same {@link KeyValueRow} shape. Params additionally stays in sync
   * with the URL field via {@link RequestTabViewModel#queryParams}; that sync lives entirely in
   * the view model, so this method doesn't need to know or care which list it was handed.
   */
  private VBox buildKeyValueEditor(final ObservableList<KeyValueRow> rows, final String idPrefix) {
    final var table = buildKeyValueTable(rows, idPrefix);
    VBox.setVgrow(table, Priority.ALWAYS);

    final var addBtn = new Button("+ Add");
    addBtn.setId(idPrefix + "AddBtn");
    addBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
    addBtn.setOnAction(_ -> rows.add(new KeyValueRow(true, "", "")));

    final var box = new VBox(8, table, addBtn);
    box.setId(idPrefix + "Editor");
    box.setPadding(new Insets(12));
    VBox.setVgrow(box, Priority.ALWAYS);
    return box;
  }

  private TableView<KeyValueRow> buildKeyValueTable(
      final ObservableList<KeyValueRow> rows, final String idPrefix) {
    final var table = new TableView<>(rows);
    table.setId(idPrefix + "Table");
    table.setEditable(true);
    table.getStyleClass().add(Styles.TEXT_SMALL);

    final var enabledCol = new TableColumn<KeyValueRow, Boolean>("");
    enabledCol.setCellValueFactory(data -> data.getValue().enabledProperty());
    enabledCol.setCellFactory(CheckBoxTableCell.forTableColumn(enabledCol));
    enabledCol.setEditable(true);
    enabledCol.setResizable(false);
    enabledCol.setPrefWidth(32);

    final var keyCol = new TableColumn<KeyValueRow, String>("Key");
    keyCol.setCellValueFactory(data -> data.getValue().keyProperty());
    keyCol.setCellFactory(TextFieldTableCell.forTableColumn());
    keyCol.setEditable(true);
    keyCol.setPrefWidth(180);

    final var valueCol = new TableColumn<KeyValueRow, String>("Value");
    valueCol.setCellValueFactory(data -> data.getValue().valueProperty());
    valueCol.setCellFactory(TextFieldTableCell.forTableColumn());
    valueCol.setEditable(true);
    valueCol.setPrefWidth(320);

    final var deleteCol = new TableColumn<KeyValueRow, Void>("");
    deleteCol.setCellFactory(_ -> deleteRowCell(rows));
    deleteCol.setResizable(false);
    deleteCol.setPrefWidth(36);

    table.getColumns().addAll(enabledCol, keyCol, valueCol, deleteCol);
    table.setMaxHeight(Double.MAX_VALUE);
    table.setMaxWidth(Double.MAX_VALUE);
    return table;
  }

  /** A "✕" button cell that removes its own row from {@code rows} when clicked. */
  private TableCell<KeyValueRow, Void> deleteRowCell(final ObservableList<KeyValueRow> rows) {
    return new TableCell<>() {
      private final Button deleteBtn = new Button("✕");

      {
        deleteBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.SMALL);
        deleteBtn.setOnAction(_ -> rows.remove(getTableRow().getItem()));
      }

      @Override
      protected void updateItem(final Void item, final boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : deleteBtn);
      }
    };
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
   * A small "Tree | Raw" segmented control that picks how a JSON response is displayed. Only
   * meaningful for JSON - XML bodies have no tree yet, so {@link #updateResponseViews()} hides this
   * entirely for XML and shows only the raw view; it's hidden for plain text/scalar bodies too,
   * since neither view applies there. Raw starts selected: it shows the response exactly as the
   * server sent it, which is the more legible default before the user has any reason to trust a
   * tree built from it.
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
    rawToggleBtn.setSelected(true);

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
   * empty state, and bodies that are neither valid JSON nor valid XML), the collapsible JSON tree,
   * or the highlighted raw text (JSON or XML) - {@link #updateVisibleView()} toggles which one is
   * visible.
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

        final var level = getTreeView() == null ? 0 : getTreeView().getTreeItemLevel(getTreeItem());
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

  /**
   * Rebuilds the tree and the raw view for a new response, then shows whichever is selected. Reads
   * both {@link RequestTabViewModel#responseJson} and {@link RequestTabViewModel#responseXml}
   * directly rather than taking a parameter, since either one (or neither) may have just changed
   * and this needs to decide {@link #bodyKind} from their combined state.
   */
  private void updateResponseViews() {
    final var json = vm.responseJson.get();
    final var xml = vm.responseXml.get();
    bodyKind = resolveBodyKind(json, xml);
    currentJson = bodyKind == BodyKind.JSON ? json : null;
    currentXmlDoc = bodyKind == BodyKind.XML ? xml : null;
    rawFoldedNodes.clear();
    xmlFoldedNodes.clear();
    responseTree.setRoot(bodyKind == BodyKind.JSON ? JsonTreeBuilder.build("root", json) : null);
    refreshRawView();
    // The Tree/Raw toggle only makes sense for JSON - XML has no tree view yet.
    viewToggleBox.setVisible(bodyKind == BodyKind.JSON);
    viewToggleBox.setManaged(bodyKind == BodyKind.JSON);
    updateVisibleView();
  }

  /**
   * JSON wins when the body is a valid object/array; otherwise a parsed XML doc wins; else none.
   */
  private static BodyKind resolveBodyKind(final JsonNode json, final Document xml) {
    if (json != null && (json.isObject() || json.isArray())) {
      return BodyKind.JSON;
    }
    if (xml != null) {
      return BodyKind.XML;
    }
    return BodyKind.NONE;
  }

  /** Re-renders the raw view from {@link #currentJson}/{@link #currentXmlDoc} and fold state. */
  private void refreshRawView() {
    final List<Text> nodes =
        switch (bodyKind) {
          case JSON -> buildRawJsonNodes(currentJson);
          case XML -> buildRawXmlNodes(currentXmlDoc);
          case NONE -> List.of();
        };
    rawJsonFlow.getChildren().setAll(nodes);
  }

  private void updateVisibleView() {
    final var showTree = bodyKind == BodyKind.JSON && !rawModeSelected;
    final var showRaw = bodyKind == BodyKind.XML || (bodyKind == BodyKind.JSON && rawModeSelected);
    responseTree.setVisible(showTree);
    responseTree.setManaged(showTree);
    rawJsonScroll.setVisible(showRaw);
    rawJsonScroll.setManaged(showRaw);
    responseArea.setVisible(bodyKind == BodyKind.NONE);
    responseArea.setManaged(bodyKind == BodyKind.NONE);
  }

  /**
   * Renders a JSON value as pretty-printed, colored {@link Text} runs - punctuation (braces,
   * brackets, colons, commas) is muted so it reads as structure rather than content, while keys and
   * values keep the same palette as the tree. This reproduces the response exactly as the API sent
   * it, unlike the tree's per-row size previews, for anyone who wants to see the whole document at
   * once. Every object/array's opening bracket is itself a clickable {@link #foldToggle(JsonNode,
   * String)} that collapses it to a "{…}"/"[…]" placeholder, the same gesture a code editor's
   * gutter fold icon offers, without needing a gutter here.
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
   * A clickable "{" or "[" that toggles its own container's fold state and re-renders the raw view.
   * IDs are assigned in traversal order ("fold-0", "fold-1", …) purely so tests can target a
   * specific container without depending on JSON key names, which may contain characters a CSS id
   * selector can't express.
   */
  private Text foldToggle(final JsonNode node, final String openChar) {
    return makeFoldToggle(
        openChar,
        JSON_CONTAINER_COLOR,
        () -> {
          rawFoldedNodes.put(node, !isFolded(node));
          refreshRawView();
        });
  }

  /**
   * Builds a clickable fold-toggle {@link Text} shared by the JSON brace/bracket toggle and the XML
   * tag-name toggle. Rendered noticeably larger and bolder than the surrounding punctuation - at
   * normal text size a lone brace or a short tag name reads as inert, too small to comfortably aim
   * at and easy to mistake for plain structure rather than a control - and it swaps to the accent
   * color with an underline on hover so the pointer confirms it's interactive before the click even
   * lands.
   */
  private Text makeFoldToggle(final String label, final Color baseColor, final Runnable onToggle) {
    final var text = new Text(label);
    text.setId("fold-" + foldIdSeq++);
    text.setFill(baseColor);
    text.setStyle(FOLD_TOGGLE_STYLE);
    text.setCursor(Cursor.HAND);
    text.setOnMouseEntered(
        _ -> {
          text.setFill(FOLD_TOGGLE_HOVER_COLOR);
          text.setUnderline(true);
        });
    text.setOnMouseExited(
        _ -> {
          text.setFill(baseColor);
          text.setUnderline(false);
        });
    text.setOnMouseClicked(_ -> onToggle.run());
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

  /**
   * Renders an XML document as pretty-printed, colored {@link Text} runs, mirroring {@link
   * #buildRawJsonNodes(JsonNode)}: tag names are colored like JSON keys, attribute values and leaf
   * text like JSON strings, and punctuation is muted. An element with child elements is a
   * "container" - its tag name becomes a clickable {@link #xmlFoldToggle(Element, String)} that
   * collapses it to a "&lt;tag&gt;…&lt;/tag&gt;" placeholder, same as JSON's braces. An element
   * with no child elements is a leaf: its own text content (if any) is rendered inline and it has
   * nothing to fold. Mixed content - text alongside child elements in the same element - isn't a
   * shape real API responses tend to have, so direct text on a container element is ignored rather
   * than chased for pixel-perfect round-tripping.
   */
  private List<Text> buildRawXmlNodes(final Document doc) {
    foldIdSeq = 0;
    final var out = new ArrayList<Text>();
    appendXml(out, doc.getDocumentElement(), 0);
    return out;
  }

  private void appendXml(final List<Text> out, final Element element, final int depth) {
    final var children = xmlChildElements(element);
    appendPunct(out, "<");
    out.add(
        children.isEmpty() ? plainTagName(element) : xmlFoldToggle(element, element.getTagName()));
    appendXmlAttributes(out, element);

    if (children.isEmpty()) {
      final var text = xmlDirectText(element);
      if (text.isEmpty()) {
        appendPunct(out, "/>");
        return;
      }
      appendPunct(out, ">");
      appendColored(out, text, JSON_STRING_COLOR);
      appendPunct(out, "</");
      appendColored(out, element.getTagName(), JSON_KEY_COLOR);
      appendPunct(out, ">");
      return;
    }

    appendPunct(out, ">");
    if (isXmlFolded(element)) {
      appendPunct(out, "…</");
      appendColored(out, element.getTagName(), JSON_KEY_COLOR);
      appendPunct(out, ">");
      return;
    }
    appendPlain(out, "\n");
    for (var i = 0; i < children.size(); i++) {
      appendPlain(out, indent(depth + 1));
      appendXml(out, children.get(i), depth + 1);
      appendPlain(out, "\n");
    }
    appendPlain(out, indent(depth));
    appendPunct(out, "</");
    appendColored(out, element.getTagName(), JSON_KEY_COLOR);
    appendPunct(out, ">");
  }

  private Text plainTagName(final Element element) {
    final var text = new Text(element.getTagName());
    text.setFill(JSON_KEY_COLOR);
    return text;
  }

  private void appendXmlAttributes(final List<Text> out, final Element element) {
    final var attributes = element.getAttributes();
    for (var i = 0; i < attributes.getLength(); i++) {
      final var attribute = (Attr) attributes.item(i);
      appendPlain(out, " ");
      appendColored(out, attribute.getName(), JSON_SCALAR_COLOR);
      appendPunct(out, "=");
      appendColored(out, "\"" + attribute.getValue() + "\"", JSON_STRING_COLOR);
    }
  }

  private static List<Element> xmlChildElements(final Element element) {
    final var out = new ArrayList<Element>();
    final var nodes = element.getChildNodes();
    for (var i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i) instanceof Element child) {
        out.add(child);
      }
    }
    return out;
  }

  private static String xmlDirectText(final Element element) {
    final var text = new StringBuilder();
    final var nodes = element.getChildNodes();
    for (var i = 0; i < nodes.getLength(); i++) {
      final var node = nodes.item(i);
      if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
        text.append(node.getNodeValue());
      }
    }
    return text.toString().trim();
  }

  private boolean isXmlFolded(final Element element) {
    return xmlFoldedNodes.getOrDefault(element, Boolean.FALSE);
  }

  private Text xmlFoldToggle(final Element element, final String tagName) {
    return makeFoldToggle(
        tagName,
        JSON_KEY_COLOR,
        () -> {
          xmlFoldedNodes.put(element, !isXmlFolded(element));
          refreshRawView();
        });
  }

  private void setupBindings() {
    methodCombo.setItems(vm.methods);
    methodCombo.valueProperty().bindBidirectional(vm.method);

    urlField.textProperty().bindBidirectional(vm.url);
    requestBodyArea.textProperty().bindBidirectional(vm.requestBody);

    responseArea.textProperty().bind(vm.responseBody);
    statusLabel.textProperty().bind(vm.statusText);
    timeLabel.textProperty().bind(vm.timeText);

    vm.responseJson.addListener((_, _, _) -> updateResponseViews());
    vm.responseXml.addListener((_, _, _) -> updateResponseViews());
    updateResponseViews();

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
