package github.com.camilyed.jbolt;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MainController {

    @FXML
    private ComboBox<String> methodCombo;
    @FXML
    private TextField urlField;
    @FXML
    private TextArea responseArea;
    @FXML
    private TreeView<String> collectionTree;

    @FXML
    public void initialize() {
        if (methodCombo != null) {
            methodCombo.setItems(FXCollections.observableArrayList("GET", "POST", "PUT", "DELETE", "PATCH"));
            methodCombo.getSelectionModel().selectFirst();
        }

        if (collectionTree != null) {
            TreeItem<String> root = new TreeItem<>("Collections");
            root.setExpanded(true);
            collectionTree.setRoot(root);
        }
    }

    @FXML
    protected void onSendRequest() {
        String url = urlField.getText();
        String method = methodCombo.getValue();
        responseArea.appendText("Sending " + method + " to: " + url + "\n");
    }
}