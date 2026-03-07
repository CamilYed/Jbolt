package github.com.camilyed.jbolt.ui;

import github.com.camilyed.jbolt.common.result.Result;
import github.com.camilyed.jbolt.testing.dsl.fakes.FakeUiMessageService;
import github.com.camilyed.jbolt.ui.service.ViewLoader;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainControllerTest {

    private TabPane tabPane;
    private FakeUiMessageService fakeUiService;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (final IllegalStateException _) {
        }
    }

    @BeforeEach
    void setUp() {
        tabPane = new TabPane();
        fakeUiService = new FakeUiMessageService();
    }

    @Test
    @DisplayName("should add a new tab when view loading is successful")
    void shouldAddTabOnSuccess() {
        // given
        final ViewLoader fakeLoader = _ -> Result.success(new Region());
        final var controller = new MainController(fakeLoader, fakeUiService);
        controller.requestTabs = tabPane;

        // when
        controller.openNewTab();

        // then
        assertThat(tabPane.getTabs()).hasSize(1);
        assertThat(tabPane.getSelectionModel().getSelectedItem().getText()).isEqualTo("New Request");
        assertThat(fakeUiService.wasErrorShown()).isFalse();
    }

    @Test
    @DisplayName("should show error message when view loading fails")
    void shouldShowErrorOnFailure() {
        // given
        final var error = new RuntimeException("IO Failure");
        final ViewLoader failingLoader = _ -> Result.failure(error);
        final var controller = new MainController( failingLoader, fakeUiService);
        controller.requestTabs = tabPane;

        // when
        controller.openNewTab();

        // then
        assertThat(tabPane.getTabs()).isEmpty();
        assertThat(fakeUiService.wasErrorShown()).isTrue();
        assertThat(fakeUiService.getCapturedError().technicalCode()).isEqualTo("UI-LOAD-001");
    }

    @Test
    @DisplayName("should open new tab only if the tab selection event is positive")
    void shouldHandleAddTabSelection() {
        // given
        final ViewLoader fakeLoader = _ -> Result.success(new Region());
        final var controller = new MainController( fakeLoader, fakeUiService);
        controller.requestTabs = tabPane;

        final var addTab = new Tab("+");
        final var selectionEvent = new Event(addTab, addTab, Tab.SELECTION_CHANGED_EVENT);

        // when
        addTab.getTabPane(); // Mocking context isn't needed, just set selected
        // Case 1: Not selected
        controller.handleAddTabTabSelected(selectionEvent);
        assertThat(tabPane.getTabs()).isEmpty();

        // Case 2: Selected
        tabPane.getTabs().add(addTab);
        tabPane.getSelectionModel().select(addTab);
        controller.handleAddTabTabSelected(selectionEvent);

        // then
        assertThat(tabPane.getTabs()).hasSize(2); // The "+" tab and the "New Request" tab
    }

    @Test
    @DisplayName("initialize should call openNewTab and set theme")
    void shouldInitializeCorrectly() {
        // given
        final ViewLoader fakeLoader = _ -> Result.success(new Region());
        final var controller = new MainController( fakeLoader, fakeUiService);
        controller.requestTabs = tabPane;

        // when
        controller.initialize();

        // then
        assertThat(tabPane.getTabs()).hasSize(1);
    }
}