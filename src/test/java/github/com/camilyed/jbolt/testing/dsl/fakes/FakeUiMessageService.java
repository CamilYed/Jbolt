package github.com.camilyed.jbolt.testing.dsl.fakes;

import github.com.camilyed.jbolt.ui.model.UiError;
import github.com.camilyed.jbolt.ui.service.UiMessageService;

public final class FakeUiMessageService extends UiMessageService {
    private UiError capturedError;

    @Override
    public void showError(final UiError error) {
        // Do not open a real JavaFX Alert in tests
        this.capturedError = error;
    }

    public UiError getCapturedError() {
        return capturedError;
    }

    public boolean wasErrorShown() {
        return capturedError != null;
    }
}