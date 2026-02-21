package github.com.camilyed.jbolt.ui.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.common.result.Result;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ViewModel for the request tab using the Result pattern and reactive properties.
 */
public final class RequestTabViewModel {

    private final RequestExecutionService service;
    private final ObjectMapper mapper = new ObjectMapper();

    // --- INPUT PROPERTIES ---
    public final StringProperty url = new SimpleStringProperty("");
    public final ObjectProperty<HttpMethod> method = new SimpleObjectProperty<>(HttpMethod.GET);
    public final StringProperty requestBody = new SimpleStringProperty("");

    // --- OUTPUT PROPERTIES ---
    public final StringProperty responseBody = new SimpleStringProperty("");
    public final StringProperty statusText = new SimpleStringProperty("---");
    public final StringProperty timeText = new SimpleStringProperty("--- ms");
    public final StringProperty statusClass = new SimpleStringProperty("");

    // --- UI STATE ---
    public final BooleanProperty loading = new SimpleBooleanProperty(false);
    public final ObservableList<HttpMethod> methods = FXCollections.observableArrayList(HttpMethod.values());

    public RequestTabViewModel(final RequestExecutionService service) {
        this.service = service;
    }

    public void sendRequest() {
        if (url.get() == null || url.get().isBlank()) {
            return;
        }

        loading.set(true);

        CompletableFuture.supplyAsync(this::executeRequest)
                .thenAccept(result -> Platform.runLater(() -> handleResult(result)))
                .whenComplete((_, _) -> Platform.runLater(() -> loading.set(false)));
    }

    private Result<HttpResponse> executeRequest() {
        return service.execute(
                url.get(),
                method.get(),
                Map.of("Accept", "application/json"),
                requestBody.get()
        );
    }

    private void handleResult(final Result<HttpResponse> result) {
        switch (result) {
            case Result.Success(var response) -> handleSuccess(response);
            case Result.Failure(var error) -> handleError(error);
        }
    }

    private void handleSuccess(final HttpResponse resp) {
        statusText.set(String.valueOf(resp.statusCode()));
        timeText.set(resp.durationMillis() + " ms");
        statusClass.set(resp.isSuccessful() ? "success" : "danger");
        renderResponseBody(resp);
    }

    private void renderResponseBody(final HttpResponse resp) {
        try {
            final var body = resp.body();
            if (body != null && !body.isBlank()) {
                final var json = mapper.readValue(body, Object.class);
                responseBody.set(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
            } else {
                responseBody.set("[Empty Response]");
            }
        } catch (final Exception _) {
            responseBody.set(resp.body());
        }
    }

    private void handleError(final Throwable ex) {
        statusText.set("ERROR");
        statusClass.set("danger");
        timeText.set("---");

        final var cause = ex.getCause() != null ? ex.getCause() : ex;
        responseBody.set("Execution Failed:\n" + cause.getMessage());
    }
}