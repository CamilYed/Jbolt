package github.com.camilyed.jbolt.ui;

import static org.assertj.core.api.Assertions.assertThat;

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
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RequestTabControllerTest {

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
   * {@code SplitPane}/{@code TabPane} are {@code Control}s: their item nodes aren't exposed to
   * {@code Node.lookup()} until a {@code Skin} exists, which normally only happens once a node is
   * part of a shown {@code Scene}. Attaching a {@code Scene} and forcing a CSS+layout pass gets the
   * skins created synchronously, without needing an actual {@code Stage}.
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
}
