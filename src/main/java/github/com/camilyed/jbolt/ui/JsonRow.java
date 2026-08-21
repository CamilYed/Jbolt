package github.com.camilyed.jbolt.ui;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One row of a JSON tree view: the key (or array index) that led to this node, and the node itself.
 * {@link #valuePreview()} renders a short, single-line summary suitable for a tree cell -
 * containers show their size instead of their full contents, since those are shown by expanding the
 * row's children, and long strings are truncated so one wide field can't blow out the row height.
 */
public record JsonRow(String key, JsonNode node) {

  private static final int MAX_STRING_PREVIEW = 120;

  public static JsonRow of(final String key, final JsonNode node) {
    return new JsonRow(key, node);
  }

  public boolean isContainer() {
    return node.isObject() || node.isArray();
  }

  public String valuePreview() {
    if (node.isObject()) {
      return "{" + node.size() + "}";
    }
    if (node.isArray()) {
      return "[" + node.size() + "]";
    }
    if (node.isTextual()) {
      return "\"" + truncate(node.asText()) + "\"";
    }
    if (node.isNull()) {
      return "null";
    }
    return node.asText();
  }

  private static String truncate(final String text) {
    return text.length() > MAX_STRING_PREVIEW ? text.substring(0, MAX_STRING_PREVIEW) + "…" : text;
  }
}
