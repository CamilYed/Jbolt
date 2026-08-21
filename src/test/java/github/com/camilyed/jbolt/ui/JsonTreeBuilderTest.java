package github.com.camilyed.jbolt.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonTreeBuilderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeAll
  static void initJavaFX() {
    try {
      Platform.startup(() -> {});
    } catch (final IllegalStateException _) {
      // Already initialized by another test class
    }
  }

  @Test
  @DisplayName("should create one child per object field, in field order")
  void shouldCreateChildPerField() throws Exception {
    // given
    final var json = MAPPER.readTree("{\"id\":1,\"title\":\"Mascara\"}");

    // when
    final var root = JsonTreeBuilder.build("root", json);

    // then
    assertThat(root.getChildren()).hasSize(2);
    assertThat(root.getChildren().get(0).getValue().key()).isEqualTo("id");
    assertThat(root.getChildren().get(1).getValue().key()).isEqualTo("title");
  }

  @Test
  @DisplayName("should index array elements by position")
  void shouldIndexArrayElements() throws Exception {
    // given
    final var json = MAPPER.readTree("[10,20,30]");

    // when
    final var root = JsonTreeBuilder.build("root", json);

    // then
    assertThat(root.getChildren()).hasSize(3);
    assertThat(root.getChildren().get(1).getValue().key()).isEqualTo("[1]");
    assertThat(root.getChildren().get(1).getValue().node().asInt()).isEqualTo(20);
  }

  @Test
  @DisplayName("should recurse into nested objects")
  void shouldRecurseNestedObjects() throws Exception {
    // given
    final var json = MAPPER.readTree("{\"dimensions\":{\"width\":1,\"height\":2}}");

    // when
    final var root = JsonTreeBuilder.build("root", json);

    // then
    final var dimensions = root.getChildren().getFirst();
    assertThat(dimensions.getChildren()).hasSize(2);
  }

  @Test
  @DisplayName("should leave scalar leaves without children")
  void shouldLeaveLeavesWithoutChildren() throws Exception {
    // given
    final var json = MAPPER.readTree("{\"ok\":true}");

    // when
    final var root = JsonTreeBuilder.build("root", json);

    // then
    assertThat(root.getChildren().getFirst().getChildren()).isEmpty();
  }

  @Test
  @DisplayName("should start the root item expanded so its direct children are visible")
  void shouldExpandRoot() throws Exception {
    // given
    final var json = MAPPER.readTree("{\"a\":1}");

    // when
    final var root = JsonTreeBuilder.build("root", json);

    // then
    assertThat(root.isExpanded()).isTrue();
  }

  @Test
  @DisplayName("should start nested containers collapsed by default")
  void shouldCollapseNestedContainers() throws Exception {
    // given
    final var json = MAPPER.readTree("{\"dimensions\":{\"width\":1}}");

    // when
    final var root = JsonTreeBuilder.build("root", json);

    // then
    assertThat(root.getChildren().getFirst().isExpanded()).isFalse();
  }
}
