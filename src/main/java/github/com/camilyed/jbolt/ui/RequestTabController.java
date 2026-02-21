package github.com.camilyed.jbolt.ui;

import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.ui.vm.RequestTabViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for the request tab.
 * Relies entirely on the provided ViewModel for state and logic.
 */
public final class RequestTabController {

    @FXML private ComboBox<HttpMethod> methodCombo;
    @FXML private TextField urlField;
    @FXML private TextArea requestBodyArea;
    @FXML private TextArea responseArea;
    @FXML private Label statusLabel;
    @FXML private Label timeLabel;
    @FXML private Button sendBtn;

    private final RequestTabViewModel vm;

    public RequestTabController(final RequestTabViewModel vm) {
        this.vm = vm;
    }

    @FXML
    public void initialize() {
        setupBindings();
    }

    private void setupBindings() {
        methodCombo.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(final HttpMethod item, final boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name());
            }
        });
        methodCombo.setItems(vm.methods);
        methodCombo.valueProperty().bindBidirectional(vm.method);

        urlField.textProperty().bindBidirectional(vm.url);
        requestBodyArea.textProperty().bindBidirectional(vm.requestBody);

        responseArea.textProperty().bind(vm.responseBody);
        statusLabel.textProperty().bind(vm.statusText);
        timeLabel.textProperty().bind(vm.timeText);

        vm.statusClass.addListener((_, oldClass, newClass) -> {
            if (oldClass != null && !oldClass.isEmpty()) {
                statusLabel.getStyleClass().remove(oldClass);
            }
            if (newClass != null && !newClass.isEmpty()) {
                statusLabel.getStyleClass().add(newClass);
            }
        });

        sendBtn.disableProperty().bind(vm.loading.or(vm.url.isEmpty()));
    }

    @FXML
    private void onSend() {
        vm.sendRequest();
    }
}