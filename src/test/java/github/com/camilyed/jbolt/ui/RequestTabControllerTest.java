package github.com.camilyed.jbolt.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.testing.dsl.fakes.FakeHttpEngine;
import github.com.camilyed.jbolt.ui.model.RequestTabViewModel;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
  @DisplayName("should switch to the json tree and hide the text area for object responses")
  void shouldShowJsonTreeForObjectResponse() throws Exception {
    // given
    final var root = realized(controller);
    final var responseArea = (TextArea) root.lookup("#responseArea");
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");

    // when
    vm.responseJson.set(MAPPER.readTree("{\"id\":1,\"title\":\"Mascara\"}"));

    // then
    assertThat(responseTree.isVisible()).isTrue();
    assertThat(responseArea.isVisible()).isFalse();
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
  @DisplayName("should show the view toggle and default to the tree for object responses")
  void shouldShowViewToggleAndDefaultToTree() throws Exception {
    // given
    final var root = realized(controller);
    final var viewToggleBox = root.lookup("#viewToggleBox");
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");
    final var rawJsonView = root.lookup("#rawJsonView");

    // when
    vm.responseJson.set(MAPPER.readTree("{\"id\":1}"));

    // then
    assertThat(viewToggleBox.isVisible()).isTrue();
    assertThat(responseTree.isVisible()).isTrue();
    assertThat(rawJsonView.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should switch to the highlighted raw view when the Raw toggle is clicked")
  void shouldSwitchToRawViewOnToggle() throws Exception {
    // given
    final var root = realized(controller);
    vm.responseJson.set(MAPPER.readTree("{\"id\":1}"));
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");
    final var rawJsonView = root.lookup("#rawJsonView");
    final var rawToggleBtn = (ToggleButton) root.lookup("#rawToggleBtn");

    // when
    rawToggleBtn.fire();

    // then
    assertThat(rawJsonView.isVisible()).isTrue();
    assertThat(responseTree.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should switch back to the tree when the Tree toggle is clicked again")
  void shouldSwitchBackToTreeOnToggle() throws Exception {
    // given
    final var root = realized(controller);
    vm.responseJson.set(MAPPER.readTree("{\"id\":1}"));
    final var responseTree = (TreeView<?>) root.lookup("#responseTree");
    final var rawJsonView = root.lookup("#rawJsonView");
    final var rawToggleBtn = (ToggleButton) root.lookup("#rawToggleBtn");
    final var treeToggleBtn = (ToggleButton) root.lookup("#treeToggleBtn");
    rawToggleBtn.fire();

    // when
    treeToggleBtn.fire();

    // then
    assertThat(responseTree.isVisible()).isTrue();
    assertThat(rawJsonView.isVisible()).isFalse();
  }

  @Test
  @DisplayName("should collapse a container to a placeholder when its fold toggle is clicked")
  void shouldFoldContainerInRawView() throws Exception {
    // given
    final var root = realized(controller);
    vm.responseJson.set(MAPPER.readTree("{\"dimensions\":{\"width\":1,\"height\":2}}"));
    final var rawToggleBtn = (ToggleButton) root.lookup("#rawToggleBtn");
    rawToggleBtn.fire();
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
    final var rawToggleBtn = (ToggleButton) root.lookup("#rawToggleBtn");
    rawToggleBtn.fire();
    final var rawJsonFlow = (TextFlow) root.lookup("#rawJsonFlow");
    final var childCountBeforeFold = rawJsonFlow.getChildren().size();
    rawJsonFlow.lookup("#fold-1").getOnMouseClicked().handle(null);

    // when
    rawJsonFlow.lookup("#fold-1").getOnMouseClicked().handle(null);

    // then
    assertThat(rawJsonFlow.getChildren().size()).isEqualTo(childCountBeforeFold);
  }
}
