package github.com.camilyed.jbolt.ui.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.common.result.Result;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/** ViewModel for the request tab using the Result pattern and reactive properties. */
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
  // Parsed form of responseBody, set only when the body parses as JSON - the view uses this to
  // render a collapsible, syntax-highlighted tree/raw view instead of a flat text block. Null for
  // XML bodies, non-JSON bodies, empty responses, and failures.
  public final ObjectProperty<JsonNode> responseJson = new SimpleObjectProperty<>();
  // Parsed form of responseBody, set only when the body parses as XML - drives the same
  // highlighted raw view as responseJson, minus the tree (there's no XML equivalent of
  // JsonTreeBuilder yet). Null for JSON bodies, non-XML bodies, empty responses, and failures.
  public final ObjectProperty<Document> responseXml = new SimpleObjectProperty<>();

  // --- UI STATE ---
  public final BooleanProperty loading = new SimpleBooleanProperty(false);
  public final ObservableList<HttpMethod> methods =
      FXCollections.observableArrayList(HttpMethod.values());

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
        url.get(), method.get(), Map.of("Accept", "application/json"), requestBody.get());
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
    final var body = resp.body();
    if (body == null || body.isBlank()) {
      responseBody.set("[Empty Response]");
      responseJson.set(null);
      responseXml.set(null);
      return;
    }
    if (looksLikeXml(resp.headers(), body)) {
      renderXmlBody(body);
    } else {
      renderJsonBody(body);
    }
  }

  /**
   * Picks JSON vs XML rendering for the response body. The Content-Type header decides when present
   * and unambiguous; otherwise this falls back to sniffing the body's first non-blank character,
   * since plenty of real APIs (and every fake/test double) omit or mislabel it.
   */
  private static boolean looksLikeXml(final Map<String, String> headers, final String body) {
    final var contentType =
        headers.entrySet().stream()
            .filter(entry -> entry.getKey().equalsIgnoreCase("Content-Type"))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("")
            .toLowerCase(Locale.ROOT);
    if (contentType.contains("xml")) {
      return true;
    }
    if (contentType.contains("json")) {
      return false;
    }
    return body.stripLeading().startsWith("<");
  }

  private void renderJsonBody(final String body) {
    try {
      final var json = mapper.readTree(body);
      responseBody.set(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
      responseJson.set(json);
      responseXml.set(null);
    } catch (final Exception _) {
      responseBody.set(body);
      responseJson.set(null);
      responseXml.set(null);
    }
  }

  private void renderXmlBody(final String body) {
    try {
      final var doc = parseXml(body);
      responseBody.set(body);
      responseXml.set(doc);
      responseJson.set(null);
    } catch (final Exception _) {
      responseBody.set(body);
      responseJson.set(null);
      responseXml.set(null);
    }
  }

  /**
   * Parses XML with DTDs and external entities disabled - this document comes straight from
   * whatever server the user just pointed the client at, so it's untrusted input and gets hardened
   * against XXE regardless of how unlikely a malicious response seems in practice.
   */
  private static Document parseXml(final String body) throws Exception {
    final var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    final var builder = factory.newDocumentBuilder();
    return builder.parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
  }

  private void handleError(final Throwable ex) {
    statusText.set("ERROR");
    statusClass.set("danger");
    timeText.set("---");
    responseJson.set(null);
    responseXml.set(null);

    final var cause = ex.getCause() != null ? ex.getCause() : ex;
    responseBody.set("Execution Failed:\n" + cause.getMessage());
  }
}
