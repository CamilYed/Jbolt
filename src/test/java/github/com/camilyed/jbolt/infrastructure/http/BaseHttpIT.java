package github.com.camilyed.jbolt.infrastructure.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.options;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import github.com.camilyed.jbolt.testing.dsl.JsonTestDataBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for all HTTP Integration Tests. Starts WireMock once per test suite execution for
 * performance.
 */
public abstract class BaseHttpIT {

  protected static WireMockServer wireMock;

  @BeforeAll
  static void setupSpec() {
    wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    wireMock.start();
    configureFor("localhost", wireMock.port());
  }

  @AfterAll
  static void tearDownSpec() {
    if (wireMock != null) {
      wireMock.stop();
    }
  }

  @BeforeEach
  void reset() {
    wireMock.resetAll();
  }

  protected String getBaseUrl() {
    return "http://localhost:" + wireMock.port();
  }

  protected RemoteServerAbility givenRemoteServer() {
    return new RemoteServerAbility() {};
  }

  public interface RemoteServerAbility {

    // --- Helper to compress response to GZip ---
    private static byte[] compressGzip(byte[] data) {
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
          GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
        gzip.write(data);
        gzip.finish();
        return bos.toByteArray();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    /** Generic stub method */
    default void returns(
        String path,
        String method,
        int status,
        JsonTestDataBuilder body,
        boolean gzip,
        Map<String, String> headers) {
      MappingBuilder builder = request(method.toUpperCase(), urlEqualTo(path));
      var responseDef =
          aResponse().withStatus(status).withHeader("Content-Type", "application/json");

      if (gzip) {
        responseDef.withBody(compressGzip(body.toString().getBytes(StandardCharsets.UTF_8)));
        responseDef.withHeader("Content-Encoding", "gzip");
      } else {
        responseDef.withBody(body != null ? body.toString() : null);
      }

      if (headers != null) {
        headers.forEach(responseDef::withHeader);
      }

      builder.willReturn(responseDef);
      stubFor(builder);
    }

    default void returnsOPTIONS(String path) {
      stubFor(
          options(urlEqualTo(path))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody("")));
    }

    default void returnsHEAD(String path) {
      stubFor(
          head(urlEqualTo(path))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody(""))); // pusty body
    }

    // --- Success responses ---
    default void returnsSuccess(String path, String method, JsonTestDataBuilder body) {
      returns(path, method, 200, body, false, null);
    }

    default void returnsSuccess(
        String path, String method, JsonTestDataBuilder body, Map<String, String> headers) {
      returns(path, method, 200, body, false, headers);
    }

    default void returnsSuccessGzip(String path, String method, JsonTestDataBuilder body) {
      returns(path, method, 200, body, true, null);
    }

    // --- Convenience methods for HTTP verbs used in tests ---
    default void returnsGET(String path, JsonTestDataBuilder body) {
      returnsSuccess(path, "GET", body);
    }

    default void returnsPOST(String path, JsonTestDataBuilder body) {
      returnsSuccess(path, "POST", body);
    }

    default void returnsPUT(String path, JsonTestDataBuilder body) {
      returnsSuccess(path, "PUT", body);
    }

    default void returnsPUT(String path, JsonTestDataBuilder body, Map<String, String> headers) {
      returnsSuccess(path, "PUT", body, headers);
    }

    default void returnsDELETE(String path, JsonTestDataBuilder body) {
      returnsSuccess(path, "DELETE", body);
    }

    default void returnsPATCH(String path, JsonTestDataBuilder body) {
      returnsSuccess(path, "PATCH", body);
    }

    // --- Delayed responses ---
    default void returnsDelayed(String path, String method, int delayMs, JsonTestDataBuilder body) {
      MappingBuilder builder =
          request(method.toUpperCase(), urlEqualTo(path))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody(body.toString())
                      .withFixedDelay(delayMs));
      stubFor(builder);
    }

    // --- Error responses ---
    default void returnsError(String path, int status, String method) {
      stubFor(
          request(method.toUpperCase(), urlEqualTo(path))
              .willReturn(aResponse().withStatus(status)));
    }

    default void returnsError(String path, int status) {
      returnsError(path, status, "GET");
    }
  }
}
