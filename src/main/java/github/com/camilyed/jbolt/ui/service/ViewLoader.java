package github.com.camilyed.jbolt.ui.service;

import github.com.camilyed.jbolt.common.result.Result;
import javafx.scene.Parent;

/**
 * Builds a named view as a {@link Result}, capturing any construction failure instead of letting
 * it propagate as an unchecked exception into JavaFX event-handling code.
 */
public interface ViewLoader {

    Result<Parent> load(String viewId);
}
