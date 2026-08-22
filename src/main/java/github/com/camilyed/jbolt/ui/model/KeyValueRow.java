package github.com.camilyed.jbolt.ui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * One editable row of a key/value table - shared shape for both the request headers editor and
 * the query-params editor, since a header row and a query-param row are the same thing: an
 * enabled flag, a key, and a value. Mutable by design (unlike the rest of this codebase's
 * preference for immutable records) because {@link javafx.scene.control.TableView} edits a row in
 * place through its properties; a record would need to be replaced wholesale on every keystroke,
 * which would also break the row's identity for whichever table cell is mid-edit.
 */
public final class KeyValueRow {

  private final BooleanProperty enabled = new SimpleBooleanProperty(true);
  private final StringProperty key = new SimpleStringProperty("");
  private final StringProperty value = new SimpleStringProperty("");

  public KeyValueRow() {}

  public KeyValueRow(final boolean enabled, final String key, final String value) {
    this.enabled.set(enabled);
    this.key.set(key == null ? "" : key);
    this.value.set(value == null ? "" : value);
  }

  public BooleanProperty enabledProperty() {
    return enabled;
  }

  public StringProperty keyProperty() {
    return key;
  }

  public StringProperty valueProperty() {
    return value;
  }

  public boolean isEnabled() {
    return enabled.get();
  }

  public String getKey() {
    return key.get();
  }

  public String getValue() {
    return value.get();
  }
}
