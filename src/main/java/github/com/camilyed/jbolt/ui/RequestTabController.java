package github.com.camilyed.jbolt.ui;

import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.ui.model.RequestTabViewModel;
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
import javafx.scene.layout.VBox;

/**
 * Builds a single request tab: the method/URL/send bar, a request-body/headers editor, and a
 * response panel. Purely a view - all state and behavior lives in the injected {@link
 * RequestTabViewModel}, which this class binds its controls to.
 */
public final class RequestTabController implements Component<VBox> {

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
    root.getStyleClass().add("content-pane");

    setupBindings();
    return root;
  }

  private HBox buildRequestBar() {
    methodCombo = new ComboBox<>();
    methodCombo.setId("methodCombo");
    methodCombo.setPrefWidth(120);
    methodCombo.getStyleClass().add("button-outlined");

    urlField = new TextField();
    urlField.setId("urlField");
    urlField.setPromptText("https://api.example.com/resource");
    HBox.setHgrow(urlField, Priority.ALWAYS);

    sendBtn = new Button("SEND");
    sendBtn.setId("sendBtn");
    sendBtn.getStyleClass().add("accent");
    sendBtn.setOnAction(event -> onSend());

    final var bar = new HBox(10, methodCombo, urlField, sendBtn);
    bar.setAlignment(Pos.CENTER_LEFT);
    bar.getStyleClass().add("dense");
    return bar;
  }

  private SplitPane buildMainSplit() {
    final var split = new SplitPane(buildRequestEditor(), buildResponseCard());
    split.setOrientation(Orientation.VERTICAL);
    split.setDividerPositions(0.45);
    split.getStyleClass().add("flat");
    return split;
  }

  private TabPane buildRequestEditor() {
    requestBodyArea = new TextArea();
    requestBodyArea.setId("requestBodyArea");
    requestBodyArea.setPromptText("Request body (JSON)…");
    requestBodyArea.getStyleClass().add("monospace");
    final var bodyTab = new Tab("Body", requestBodyArea);
    bodyTab.setClosable(false);

    final var headersPlaceholder = new Label("Headers editor coming soon");
    headersPlaceholder.getStyleClass().add("text-muted");
    final var headersPane = new VBox(8, headersPlaceholder);
    headersPane.getStyleClass().add("content-pane");
    final var headersTab = new Tab("Headers", headersPane);
    headersTab.setClosable(false);

    final var tabs = new TabPane(bodyTab, headersTab);
    tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    tabs.getStyleClass().addAll("flat", "dense");
    return tabs;
  }

  private VBox buildResponseCard() {
    statusLabel = new Label("—");
    statusLabel.setId("statusLabel");
    timeLabel = new Label("— ms");
    timeLabel.setId("timeLabel");

    final var statusRow =
        new HBox(
            12,
            mutedLabel("Status"),
            statusLabel,
            new Separator(Orientation.VERTICAL),
            mutedLabel("Time"),
            timeLabel);
    statusRow.setAlignment(Pos.CENTER_LEFT);

    responseArea = new TextArea();
    responseArea.setId("responseArea");
    responseArea.setEditable(false);
    responseArea.setPromptText("Response will appear here…");
    responseArea.getStyleClass().add("monospace");
    VBox.setVgrow(responseArea, Priority.ALWAYS);

    final var card = new VBox(8, statusRow, responseArea);
    card.getStyleClass().add("card");
    return card;
  }

  private Label mutedLabel(final String text) {
    final var label = new Label(text);
    label.getStyleClass().add("text-muted");
    return label;
  }

  private void setupBindings() {
    methodCombo.setCellFactory(
        _ ->
            new ListCell<>() {
              @Override
              protected void updateItem(final HttpMethod item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
              }
            });
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

  private void onSend() {
    vm.sendRequest();
  }
}
