package github.com.camilyed.jbolt.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.testing.dsl.fakes.FakeHttpEngine;
import github.com.camilyed.jbolt.ui.model.KeyValueRow;
import github.com.camilyed.jbolt.ui.model.RequestTabViewModel;
import java.io.StringReader;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

class RequestTabControllerTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private RequestTabViewModel vm;
  private RequestTabController controller;

  @BeforeAll
  static void initJavaFX() {
    try {
      Platform.startup(() -> {});
    } catch (final IllegalStateException _) {
      // Already initialized by another test class
    }
  }

  @BeforeEach
  void setUp() {
    final var service = new RequestExecutionService(new FakeHttpEngine());
    vm = new RequestTabViewModel(service);
    controller = new RequestTabController(vm);
  }

  /**
   * {@code SplitPane}/{@code TabPane}/{@code Card} are {@code Control}s: their item nodes aren't
   * exposed to {@code Node.lookup()} until a {@code Skin} exists, which normally only happens once
   * a node is part of a shown {@code Scene}. Attaching a {@code Scene} and forcing a CSS+layout
   * pass gets the skins created synchronously, without needing an actual {@code Stage}.
   */
  private static VBox realized(final RequestTabController controller) {
    final var root = controller.build();
    new Scene(root);
    root.applyCss();
    root.layout();
    return root;
  }

  private static Document parseXml(final String xml) throws Exception {
    final var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(xml)));
  }

  @Test
  @DisplayName("should bind the url field to the view model bidirectionally")
  void shouldBindUrlFieldToViewModel() {
    // given
    final var root = realized(controller);
    final var urlField = (TextField) root.lookup("#urlField");

    // when
    urlField.setText("https://api.example.com");

    // then
    assertThat(vm.url.get()).isEqualTo("https://api.example.com");
  }

  @Test
  @DisplayName("should disable the send button while the url is empty")
  void shouldDisableSendButtonWhenUrlEmpty() {
    // given
    final var root = realized(controller);

    // when
    final var sendBtn = (Button) root.lookup("#sendBtn");

    // then
    assertThat(sendBtn.isDisabled()).isTrue();
  }

  @Test
  @DisplayName("should enable the send button once a url is provided")
  void shouldEnableSendButtonWhenUrlProvided() {
    // given
    final var root = realized(controller);
    final var urlField = (TextField) root.lookup("#urlField");
    final var sendBtn = (Button) root.lookup("#sendBtn");

    // when
    urlField.setText("https://api.example.com");

    // then
    assertThat(sendBtn.isDisabled()).isFalse();
  }

  @Test
  @DisplayName("should expose the response area bound to the view model")
  void shouldBindResponseAreaToViewModel() {
    // given
    final var root = realized(controller);
    final var responseArea = (TextArea) root.lookup("#responseArea");

    // when
    vm.responseBody.set("{ \"ok\": true }");

    // then
    assertThat(responseArea.getText()).isEqualTo("{ \"ok\": true }");
  }

  @Test
  @DisplayName("should populate the method combo box with every http method")
  void shouldPopulateMethodCombo() {
    // given
    final var root = realized(controller);

    // when
    @SuppressWarnings("unchecked")
    final var methodCombo = (ComboBox<HttpMethod>) root.lookup("#methodCombo");

    // then
    assertThat(methodCombo.getItems()).containsExactly(HttpMethod.values());
  }

  @Test
  @DisplayName("should show the plain response area and hide the json tree before any response")
  void shouldShowTextAreaBeforeAnyResponse() {
    // given
    final var root = realized(controller);
    final var responseArea = (TextArea) root.lookup("#responseArea");
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");

    // then
    assertThat(responseArea.isVisible()).isTrue();
    assertThat(responseTree.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should build the json tree and hide the text area for object responses")
  void shouldBuildJsonTreeForObjectResponse() throws Exception {
    // given
    final var root = realized(controller);
    final var responseArea = (TextArea) root.lookup("#responseArea");
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");

    // when
    vm.responseJson.set(MAPPER.readTree("{\"id\":1,\"title\":\"Mascara\"}"));

    // then
    assertThat(responseArea.isVisible()).isFalse();
    // The tree is built eagerly even though Raw is the default visible view, so switching to Tree
    // via the toggle is instant rather than triggering a rebuild.
    assertThat(responseTree.getRoot().getChildren()).hasSize(2);
  }

  @Test
  @DisplayName("should switch back to the text area once the json tree is cleared")
  void shouldFallBackToTextAreaWhenJsonCleared() throws Exception {
    // given
    final var root = realized(controller);
    final var responseArea = (TextArea) root.lookup("#responseArea");
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");
    vm.responseJson.set(MAPPER.readTree("{\"id\":1}"));

    // when
    vm.responseJson.set(null);

    // then
    assertThat(responseArea.isVisible()).isTrue();
    assertThat(responseTree.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should hide the tree/raw view toggle when there is no parsed json to switch")
  void shouldHideViewToggleWhenNoJson() {
    // given
    final var root = realized(controller);

    // then
    final var viewToggleBox = root.lookup("#viewToggleBox");
    assertThat(viewToggleBox.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should show the view toggle and default to the raw view for object responses")
  void shouldShowViewToggleAndDefaultToRaw() throws Exception {
    // given
    final var root = realized(controller);
    final var viewToggleBox = root.lookup("#viewToggleBox");
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");
    final var rawJsonView = root.lookup("#rawJsonView");

    // when
    vm.responseJson.set(MAPPER.readTree("{\"id\":1}"));

    // then
    assertThat(viewToggleBox.isVisible()).isTrue();
    assertThat(rawJsonView.isVisible()).isTrue();
    assertThat(responseTree.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should switch to the json tree when the Tree toggle is clicked")
  void shouldSwitchToTreeViewOnToggle() throws Exception {
    // given
    final var root = realized(controller);
    vm.responseJson.set(MAPPER.readTree("{\"id\":1}"));
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");
    final var rawJsonView = root.lookup("#rawJsonView");
    final var treeToggleBtn = (ToggleButton) root.lookup("#treeToggleBtn");

    // when
    treeToggleBtn.fire();

    // then
    assertThat(responseTree.isVisible()).isTrue();
    assertThat(rawJsonView.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should switch back to the raw view when the Raw toggle is clicked again")
  void shouldSwitchBackToRawViewOnToggle() throws Exception {
    // given
    final var root = realized(controller);
    vm.responseJson.set(MAPPER.readTree("{\"id\":1}"));
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");
    final var rawJsonView = root.lookup("#rawJsonView");
    final var rawToggleBtn = (ToggleButton) root.lookup("#rawToggleBtn");
    final var treeToggleBtn = (ToggleButton) root.lookup("#treeToggleBtn");
    treeToggleBtn.fire();

    // when
    rawToggleBtn.fire();

    // then
    assertThat(rawJsonView.isVisible()).isTrue();
    assertThat(responseTree.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should collapse a container to a placeholder when its fold toggle is clicked")
  void shouldFoldContainerInRawView() throws Exception {
    // given
    final var root = realized(controller);
    vm.responseJson.set(MAPPER.readTree("{\"dimensions\":{\"width\":1,\"height\":2}}"));
    final var rawJsonFlow = (TextFlow) root.lookup("#rawJsonFlow");
    final var childCountBeforeFold = rawJsonFlow.getChildren().size();
    final var dimensionsFoldToggle = rawJsonFlow.lookup("#fold-1");

    // when
    dimensionsFoldToggle.getOnMouseClicked().handle(null);

    // then
    assertThat(rawJsonFlow.getChildren().size()).isLessThan(childCountBeforeFold);
  }

  @Test
  @DisplayName("should expand a folded container again when its toggle is clicked a second time")
  void shouldUnfoldContainerInRawView() throws Exception {
    // given
    final var root = realized(controller);
    vm.responseJson.set(MAPPER.readTree("{\"dimensions\":{\"width\":1,\"height\":2}}"));
    final var rawJsonFlow = (TextFlow) root.lookup("#rawJsonFlow");
    final var childCountBeforeFold = rawJsonFlow.getChildren().size();
    rawJsonFlow.lookup("#fold-1").getOnMouseClicked().handle(null);

    // when
    rawJsonFlow.lookup("#fold-1").getOnMouseClicked().handle(null);

    // then
    assertThat(rawJsonFlow.getChildren().size()).isEqualTo(childCountBeforeFold);
  }

  @Test
  @DisplayName("should show the raw highlighted view, with no tree toggle, for an xml response")
  void shouldShowRawViewForXmlResponse() throws Exception {
    // given
    final var root = realized(controller);
    final var responseArea = (TextArea) root.lookup("#responseArea");
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");
    final var rawJsonView = root.lookup("#rawJsonView");
    final var viewToggleBox = root.lookup("#viewToggleBox");

    // when
    vm.responseXml.set(parseXml("<person><name>Alice</name></person>"));

    // then
    assertThat(rawJsonView.isVisible()).isTrue();
    assertThat(responseTree.isVisible()).isFalse();
    assertThat(responseArea.isVisible()).isFalse();
    // XML has no tree view yet, so the Tree/Raw toggle would be pointless - it stays hidden.
    assertThat(viewToggleBox.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should switch back to the text area once the xml is cleared")
  void shouldFallBackToTextAreaWhenXmlCleared() throws Exception {
    // given
    final var root = realized(controller);
    final var responseArea = (TextArea) root.lookup("#responseArea");
    final var rawJsonView = root.lookup("#rawJsonView");
    vm.responseXml.set(parseXml("<person><name>Alice</name></person>"));

    // when
    vm.responseXml.set(null);

    // then
    assertThat(responseArea.isVisible()).isTrue();
    assertThat(rawJsonView.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should collapse an xml element to a placeholder when its fold toggle is clicked")
  void shouldFoldXmlElementInRawView() throws Exception {
    // given
    final var root = realized(controller);
    vm.responseXml.set(parseXml("<root><person><name>Alice</name><age>30</age></person></root>"));
    final var rawJsonFlow = (TextFlow) root.lookup("#rawJsonFlow");
    final var childCountBeforeFold = rawJsonFlow.getChildren().size();
    // fold-0 is the <root> element itself; fold-1 is the nested <person> element.
    final var personFoldToggle = rawJsonFlow.lookup("#fold-1");

    // when
    personFoldToggle.getOnMouseClicked().handle(null);

    // then
    assertThat(rawJsonFlow.getChildren().size()).isLessThan(childCountBeforeFold);
  }

  @Test
  @DisplayName("should add a header row to the view model when + Add is clicked")
  void shouldAddHeaderRowWhenAddButtonClicked() {
    // given
    final var root = realized(controller);
    final var addBtn = (Button) root.lookup("#headersAddBtn");
    final var sizeBefore = vm.headers.size();

    // when
    addBtn.fire();

    // then
    assertThat(vm.headers).hasSize(sizeBefore + 1);
    assertThat(vm.headers.getLast().isEnabled()).isTrue();
    assertThat(vm.headers.getLast().getKey()).isEmpty();
  }

  @Test
  @DisplayName("should add a query param row to the view model when + Add is clicked")
  void shouldAddQueryParamRowWhenAddButtonClicked() {
    // given
    final var root = realized(controller);
    final var addBtn = (Button) root.lookup("#queryParamsAddBtn");
    final var sizeBefore = vm.queryParams.size();

    // when
    addBtn.fire();

    // then
    assertThat(vm.queryParams).hasSize(sizeBefore + 1);
  }

  @Test
  @DisplayName("should reflect view model headers in the headers table")
  void shouldBindHeadersTableToViewModel() {
    // given
    final var root = realized(controller);
    @SuppressWarnings("unchecked")
    final var headersTable = (TableView<KeyValueRow>) root.lookup("#headersTable");

    // when
    vm.headers.add(new KeyValueRow(true, "X-Trace-Id", "abc-123"));

    // then
    assertThat(headersTable.getItems()).hasSize(1);
    assertThat(headersTable.getItems().getFirst().getKey()).isEqualTo("X-Trace-Id");
  }

  @Test
  @DisplayName("should reflect view model query params in the params table")
  void shouldBindQueryParamsTableToViewModel() {
    // given
    final var root = realized(controller);
    @SuppressWarnings("unchecked")
    final var paramsTable = (TableView<KeyValueRow>) root.lookup("#queryParamsTable");

    // when
    vm.url.set("http://test.com/resource?filter=active");

    // then
    assertThat(paramsTable.getItems()).hasSize(1);
    assertThat(paramsTable.getItems().getFirst().getKey()).isEqualTo("filter");
  }
}
