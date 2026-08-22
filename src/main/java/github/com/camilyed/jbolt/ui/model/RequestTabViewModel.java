package github.com.camilyed.jbolt.ui.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.com.camilyed.jbolt.application.execution.RequestExecutionService;
import github.com.camilyed.jbolt.common.result.Result;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
  // User-defined request headers - only rows with isEnabled() true and a non-blank key are sent.
  public final ObservableList<KeyValueRow> headers =
      FXCollections.observableArrayList(RequestTabViewModel::rowObservables);
  // Query-string parameters, kept in sync with url in both directions: editing the URL rebuilds
  // this list, and editing a row (or adding/removing one) rebuilds the URL's query string. See
  // wireUrlQueryParamSync() for how the two directions avoid feeding back into each other.
  public final ObservableList<KeyValueRow> queryParams =
      FXCollections.observableArrayList(RequestTabViewModel::rowObservables);

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

  // Reentrancy guards for the url <-> queryParams sync: each direction sets its own flag before
  // writing the other side, and checks the other flag before reacting - without this, url.set(...)
  // inside the params listener would trigger the url listener, which would rewrite queryParams,
  // which would fire the params listener again, forever.
  private boolean syncingFromUrl;
  private boolean syncingFromParams;

  public RequestTabViewModel(final RequestExecutionService service) {
    this.service = service;
    wireUrlQueryParamSync();
  }

  private static Observable[] rowObservables(final KeyValueRow row) {
    return new Observable[] {row.enabledProperty(), row.keyProperty(), row.valueProperty()};
  }

  private void wireUrlQueryParamSync() {
    url.addListener(
        (_, _, newUrl) -> {
          if (syncingFromParams) {
            return;
          }
          syncingFromUrl = true;
          try {
            queryParams.setAll(parseQueryParams(newUrl));
          } finally {
            syncingFromUrl = false;
          }
        });
    queryParams.addListener(
        (ListChangeListener<KeyValueRow>)
            change -> {
              if (syncingFromUrl) {
                return;
              }
              syncingFromParams = true;
              try {
                url.set(rebuildUrlWithParams(url.get(), queryParams));
              } finally {
                syncingFromParams = false;
              }
            });
  }

  private static List<KeyValueRow> parseQueryParams(final String urlValue) {
    if (urlValue == null) {
      return List.of();
    }
    final var queryStart = urlValue.indexOf('?');
    if (queryStart < 0 || queryStart == urlValue.length() - 1) {
      return List.of();
    }
    final var rows = new ArrayList<KeyValueRow>();
    for (final var pair : urlValue.substring(queryStart + 1).split("&", -1)) {
      if (pair.isEmpty()) {
        continue;
      }
      final var eq = pair.indexOf('=');
      final var key = eq < 0 ? pair : pair.substring(0, eq);
      final var value = eq < 0 ? "" : pair.substring(eq + 1);
      rows.add(new KeyValueRow(true, urlDecode(key), urlDecode(value)));
    }
    return rows;
  }

  private static String rebuildUrlWithParams(final String currentUrl, final List<KeyValueRow> rows) {
    final var base = currentUrl == null ? "" : currentUrl;
    final var queryStart = base.indexOf('?');
    final var baseWithoutQuery = queryStart < 0 ? base : base.substring(0, queryStart);
    final var enabledRows =
        rows.stream().filter(KeyValueRow::isEnabled).filter(row -> !row.getKey().isBlank()).toList();
    if (enabledRows.isEmpty()) {
      return baseWithoutQuery;
    }
    final var query =
        enabledRows.stream()
            .map(row -> urlEncode(row.getKey()) + "=" + urlEncode(row.getValue()))
            .collect(Collectors.joining("&"));
    return baseWithoutQuery + "?" + query;
  }

  private static String urlEncode(final String raw) {
    return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static String urlDecode(final String raw) {
    try {
      return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    } catch (final IllegalArgumentException _) {
      return raw;
    }
  }

  public void sendRequest() {
    if (url.get() == null || url.get().isBlank()) {
      return;
    }

    loading.set(true);
    // Snapshotted on the FX thread before dispatching, same as url/method/requestBody being read
    // by value below - an ObservableList isn't safe to iterate concurrently with FX-thread edits.
    final var requestHeaders = buildRequestHeaders();

    CompletableFuture.supplyAsync(() -> executeRequest(requestHeaders))
        .thenAccept(result -> Platform.runLater(() -> handleResult(result)))
        .whenComplete((_, _) -> Platform.runLater(() -> loading.set(false)));
  }

  /**
   * Merges the user's enabled, non-blank-key header rows over the client's one default header, in
   * table order - so a user-added "Accept" row wins over the default rather than being shadowed by
   * it.
   */
  private Map<String, String> buildRequestHeaders() {
    final var result = new LinkedHashMap<String, String>();
    result.put("Accept", "application/json");
    for (final var row : headers) {
      if (row.isEnabled() && !row.getKey().isBlank()) {
        result.put(row.getKey(), row.getValue());
      }
    }
    return result;
  }

  private Result<HttpResponse> executeRequest(final Map<String, String> requestHeaders) {
    return service.execute(url.get(), method.get(), requestHeaders, requestBody.get());
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
