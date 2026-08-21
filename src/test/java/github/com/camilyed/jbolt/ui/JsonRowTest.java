package github.com.camilyed.jbolt.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonRowTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  @DisplayName("should preview a string value with quotes")
  void shouldPreviewString() throws Exception {
    // given
    final var node = MAPPER.readTree("\"hello\"");

    // when
    final var preview = JsonRow.of("name", node).valuePreview();

    // then
    assertThat(preview).isEqualTo("\"hello\"");
  }

  @Test
  @DisplayName("should preview an object as a field count")
  void shouldPreviewObject() throws Exception {
    // given
    final var node = MAPPER.readTree("{\"a\":1,\"b\":2}");

    // when
    final var preview = JsonRow.of("dimensions", node).valuePreview();

    // then
    assertThat(preview).isEqualTo("{2}");
  }

  @Test
  @DisplayName("should preview an array as an element count")
  void shouldPreviewArray() throws Exception {
    // given
    final var node = MAPPER.readTree("[1,2,3]");

    // when
    final var preview = JsonRow.of("tags", node).valuePreview();

    // then
    assertThat(preview).isEqualTo("[3]");
  }

  @Test
  @DisplayName("should preview null as the literal null")
  void shouldPreviewNull() throws Exception {
    // given
    final var node = MAPPER.readTree("null");

    // when
    final var preview = JsonRow.of("x", node).valuePreview();

    // then
    assertThat(preview).isEqualTo("null");
  }

  @Test
  @DisplayName("should preview a number without quotes")
  void shouldPreviewNumber() throws Exception {
    // given
    final var node = MAPPER.readTree("42");

    // when
    final var preview = JsonRow.of("age", node).valuePreview();

    // then
    assertThat(preview).isEqualTo("42");
  }

  @Test
  @DisplayName("should preview a boolean without quotes")
  void shouldPreviewBoolean() throws Exception {
    // given
    final var node = MAPPER.readTree("true");

    // when
    final var preview = JsonRow.of("active", node).valuePreview();

    // then
    assertThat(preview).isEqualTo("true");
  }

  @Test
  @DisplayName("should truncate long string previews so one field can't blow out the row")
  void shouldTruncateLongStrings() throws Exception {
    // given
    final var longText = "a".repeat(200);
    final var node = MAPPER.readTree("\"" + longText + "\"");

    // when
    final var preview = JsonRow.of("description", node).valuePreview();

    // then
    assertThat(preview).hasSize(123); // opening quote + 120 chars + ellipsis + closing quote
    assertThat(preview).startsWith("\"" + "a".repeat(120));
    assertThat(preview).endsWith("…\"");
  }

  @Test
  @DisplayName("should report objects and arrays as containers, scalars as not")
  void shouldReportContainer() throws Exception {
    assertThat(JsonRow.of("a", MAPPER.readTree("{}")).isContainer()).isTrue();
    assertThat(JsonRow.of("a", MAPPER.readTree("[]")).isContainer()).isTrue();
    assertThat(JsonRow.of("a", MAPPER.readTree("1")).isContainer()).isFalse();
    assertThat(JsonRow.of("a", MAPPER.readTree("\"x\"")).isContainer()).isFalse();
  }
}
