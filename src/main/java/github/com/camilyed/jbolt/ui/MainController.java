package github.com.camilyed.jbolt.ui;

import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;
import github.com.camilyed.jbolt.infrastructure.http.JavaNetHttpEngine;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Map;

public class MainController {

    @FXML private ComboBox<String> methodCombo;
    @FXML private TextField urlField;
    @FXML private TextArea responseArea;
    @FXML private TreeView<String> collectionTree;
    @FXML private Button sendBtn;

    private RequestExecutionService requestService;

    @FXML
    public void initialize() {
        methodCombo.setItems(
                FXCollections.observableArrayList("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
        );
        methodCombo.getSelectionModel().selectFirst();

        final var root = new TreeItem<>("Collections");
        root.setExpanded(true);
        collectionTree.setRoot(root);
        this.requestService = new RequestExecutionService(new JavaNetHttpEngine());
    }

    @FXML
    protected void onSendRequest() {
        final var url = urlField.getText();
        final var method = HttpMethod.valueOf(methodCombo.getValue());

        responseArea.clear();
        responseArea.appendText("Sending " + method + " to: " + url + "\n");

        final Task<HttpResponse> task = new Task<>() {
            @Override
            protected HttpResponse call() throws Exception {
                return requestService.execute(
                        url,
                        method,
                        Map.of("Content-Type", "application/json"),
                        ""
                );
            }
        };

        task.setOnSucceeded(_ -> updateUiWithResponse(task.getValue()));
        task.setOnFailed(_ -> showError(task.getException()));

        new Thread(task, "http-request-thread").start();
    }

    private void updateUiWithResponse(final HttpResponse response) {
        responseArea.appendText("\nStatus: " + response.statusCode() + "\n");
        responseArea.appendText("Duration: " + response.durationMillis() + " ms\n\n");
        responseArea.appendText(response.body());
    }

    private void showError(final Throwable error) {
        responseArea.appendText("\nERROR:\n" + error.getMessage());
    }
}
