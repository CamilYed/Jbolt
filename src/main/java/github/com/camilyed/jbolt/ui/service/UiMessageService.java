package github.com.camilyed.jbolt.ui.service;

import github.com.camilyed.jbolt.ui.model.UiError;
import javafx.scene.control.Alert;

/** Service for handling user-facing messages and technical logging. */
public class UiMessageService {

  public void showError(final UiError error) {
    // Log for developers (could be redirected to a file in AOT)
    System.err.printf(
        "[%s] %s: %s%n", error.technicalCode(), error.title(), error.cause().getMessage());

    // Display to user
    final var alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error Encountered");
    alert.setHeaderText(error.title() + " (" + error.technicalCode() + ")");
    alert.setContentText(error.message());
    alert.showAndWait();
  }
}
