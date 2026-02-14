package github.com.camilyed.jbolt.ui;

import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.infrastructure.http.JavaNetHttpEngine;
import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.ui.vm.RequestTabViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class RequestTabController {

    @FXML private ComboBox<HttpMethod> methodCombo;
    @FXML private TextField urlField;
    @FXML private TextArea requestBodyArea;
    @FXML private TextArea responseArea;
    @FXML private Label statusLabel;
    @FXML private Label timeLabel;
    @FXML private Button sendBtn;

    private RequestTabViewModel vm;

    @FXML
    public void initialize() {
        // NOTE: In a real-world scenario, this should be injected
        final var engine = new JavaNetHttpEngine();
        final var service = new RequestExecutionService(engine);
        vm = new RequestTabViewModel(service);

        setupBindings();
    }

    private void setupBindings() {
        // Method Selector
        methodCombo.setCellFactory( _ -> new ListCell<>() {
            @Override
            protected void updateItem(final HttpMethod item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        methodCombo.setItems(vm.methods);
        methodCombo.valueProperty().bindBidirectional(vm.method);

        // Input Fields
        urlField.textProperty().bindBidirectional(vm.url);
        requestBodyArea.textProperty().bindBidirectional(vm.requestBody);

        // Output Display
        responseArea.textProperty().bind(vm.responseBody);
        statusLabel.textProperty().bind(vm.statusText);
        timeLabel.textProperty().bind(vm.timeText);

        // Dynamic Styling (AtlantaFX classes)
        vm.statusClass.addListener((final var _, final var oldClass, final var newClass) -> {
            if (oldClass != null && !oldClass.isEmpty()) {
                statusLabel.getStyleClass().remove(oldClass);
            }
            if (newClass != null && !newClass.isEmpty()) {
                statusLabel.getStyleClass().add(newClass);
            }
        });

        // Controls State
        sendBtn.disableProperty().bind(vm.loading.or(vm.url.isEmpty()));
    }

    @FXML
    private void onSend() {
        vm.sendRequest();
    }
}