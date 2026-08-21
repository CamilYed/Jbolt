package github.com.camilyed.jbolt.ui;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.control.TreeItem;

/**
 * Turns a Jackson {@link JsonNode} into a {@link TreeItem} hierarchy for display in a {@code
 * TreeView<JsonRow>}. Object fields and array elements both become child rows; scalar values are
 * leaves. The root item itself is expanded so its direct children show immediately, but nested
 * containers start collapsed - the reader expands what they want to inspect instead of being shown
 * the whole document flattened out at once.
 */
public final class JsonTreeBuilder {

  private JsonTreeBuilder() {}

  public static TreeItem<JsonRow> build(final String rootLabel, final JsonNode root) {
    final var item = new TreeItem<>(JsonRow.of(rootLabel, root));
    addChildren(item, root);
    item.setExpanded(true);
    return item;
  }

  private static void addChildren(final TreeItem<JsonRow> parent, final JsonNode node) {
    if (node.isObject()) {
      node.fields()
          .forEachRemaining(
              entry -> {
                final var child = new TreeItem<>(JsonRow.of(entry.getKey(), entry.getValue()));
                addChildren(child, entry.getValue());
                parent.getChildren().add(child);
              });
    } else if (node.isArray()) {
      for (var i = 0; i < node.size(); i++) {
        final var value = node.get(i);
        final var child = new TreeItem<>(JsonRow.of("[" + i + "]", value));
        addChildren(child, value);
        parent.getChildren().add(child);
      }
    }
  }
}
