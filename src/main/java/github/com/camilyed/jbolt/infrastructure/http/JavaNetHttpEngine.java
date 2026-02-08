package github.com.camilyed.jbolt.infrastructure.http;

import github.com.camilyed.jbolt.domain.execution.HttpEngine;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpRequest;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Professional Java HTTP engine using {@link java.net.http.HttpClient}.
 * Supports all HTTP methods, headers, JSON body, gzip decoding, and timing.
 * Fully compatible with JBolt DSL and integration tests.
 */
public final class JavaNetHttpEngine implements HttpEngine {

    private static final String GZIP = "gzip";
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client;

    public JavaNetHttpEngine() {
        this(DEFAULT_TIMEOUT);
    }

    public JavaNetHttpEngine(final Duration timeout) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public HttpResponse execute(final HttpRequest request) throws Exception {
        final var start = System.currentTimeMillis();
        final var httpRequest = buildJavaNetRequest(request);
        final var rawResponse = client.send(httpRequest, BodyHandlers.ofByteArray());
        final var body = decodeBody(rawResponse);
        final var headers = extractHeaders(rawResponse);
        final var duration = System.currentTimeMillis() - start;
        return new HttpResponse(
                rawResponse.statusCode(),
                body,
                headers,
                duration
        );
    }

    private static java.net.http.HttpRequest buildJavaNetRequest(final HttpRequest request) {
        final var builder = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(request.url()))
                .timeout(DEFAULT_TIMEOUT);
        request.headers().forEach(builder::header);
        final var bodyPublisher = getBodyPublisher(request);
        applyMethod(builder, request.method(), bodyPublisher);
        return builder.build();
    }

    private static BodyPublisher getBodyPublisher(final HttpRequest request) {
        return request.body()
                .map(BodyPublishers::ofString)
                .orElseGet(BodyPublishers::noBody);
    }

    private static void applyMethod(final Builder builder, final HttpMethod method, final BodyPublisher bodyPublisher) {
        switch (method) {
            case GET -> builder.GET();
            case POST -> builder.POST(bodyPublisher);
            case PUT -> builder.PUT(bodyPublisher);
            case DELETE -> builder.DELETE();
            case PATCH -> builder.method("PATCH", bodyPublisher);
            case HEAD -> builder.method("HEAD", BodyPublishers.noBody());
            case OPTIONS -> builder.method("OPTIONS", BodyPublishers.noBody());
        }
    }

    private static String decodeBody(final java.net.http.HttpResponse<byte[]> rawResponse) throws IOException {
        final var bodyBytes = rawResponse.body();
        final var contentEncoding = rawResponse.headers().firstValue(CONTENT_ENCODING).orElse("");
        return GZIP.equalsIgnoreCase(contentEncoding)
                ? decodeGzip(bodyBytes)
                : new String(bodyBytes, StandardCharsets.UTF_8);
    }

    private static String decodeGzip(final byte[] bytes) throws IOException {
        try (final var gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
             final var baos = new ByteArrayOutputStream()) {
            final var buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> extractHeaders(final java.net.http.HttpResponse<byte[]> rawResponse) {
        return rawResponse.headers().map().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.join(",", e.getValue())
                ));
    }
}
