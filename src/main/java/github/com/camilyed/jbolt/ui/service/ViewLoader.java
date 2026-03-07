package github.com.camilyed.jbolt.ui.service;

import github.com.camilyed.jbolt.common.result.Result;
import javafx.scene.Parent;

public interface ViewLoader {
    Result<Parent> load(String fxmlPath);
}