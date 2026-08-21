package github.com.camilyed.jbolt.ui;

import atlantafx.base.controls.Card;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.ui.model.RequestTabViewModel;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

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

  private ComboBox<HttpMethod> methodCombo;
  private TextField urlField;
  private TextArea requestBodyArea;
  private TextArea responseArea;
  private Label statusLabel;
  private Label timeLabel;
  private Button sendBtn;

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

    final var statusRow =
        new HBox(10, statusLabel, new Separator(Orientation.VERTICAL), timeLabel);
    statusRow.setAlignment(Pos.CENTER_LEFT);

    responseArea = new TextArea();
    responseArea.setId("responseArea");
    responseArea.setEditable(false);
    responseArea.setPromptText("Response will appear here…");
    responseArea.getStyleClass().add(Styles.TEXT_SMALL);
    VBox.setVgrow(responseArea, Priority.ALWAYS);

    final var card = new Card();
    card.setHeader(title);
    card.setSubHeader(statusRow);
    card.setBody(responseArea);
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

  private void setupBindings() {
    methodCombo.setItems(vm.methods);
    methodCombo.valueProperty().bindBidirectional(vm.method);

    urlField.textProperty().bindBidirectional(vm.url);
    requestBodyArea.textProperty().bindBidirectional(vm.requestBody);

    responseArea.textProperty().bind(vm.responseBody);
    statusLabel.textProperty().bind(vm.statusText);
    timeLabel.textProperty().bind(vm.timeText);

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

  /** A list cell showing the method name next to a colored dot - used both for the combo's own
   * button face and for each row in its dropdown, so the selected method reads at a glance. */
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
