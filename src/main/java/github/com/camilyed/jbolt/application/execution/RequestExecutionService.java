package github.com.camilyed.jbolt.application.execution;

import github.com.camilyed.jbolt.domain.execution.HttpEngine;
import github.com.camilyed.jbolt.domain.execution.HttpMethod;
import github.com.camilyed.jbolt.domain.execution.HttpRequest;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;

import java.util.Map;

public final class RequestExecutionService {

    private final HttpEngine httpEngine;

    public RequestExecutionService(final HttpEngine httpEngine) {
        this.httpEngine = httpEngine;
    }

    public HttpResponse execute(
            final String url,
            final HttpMethod method,
            final Map<String, String> headers,
            final String body
    ) throws Exception {

        final var request = HttpRequest.builder()
                .withUrl(url)
                .withMethod(method)
                .withHeaders(headers)
                .withBody(body)
                .build();

        return httpEngine.execute(request);
    }
}
