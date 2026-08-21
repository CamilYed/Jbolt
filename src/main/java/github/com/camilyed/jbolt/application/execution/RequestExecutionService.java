package github.com.camilyed.jbolt.application.execution;

import github.com.camilyed.jbolt.common.result.Result;
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

  public Result<HttpResponse> execute(
      final String url,
      final HttpMethod method,
      final Map<String, String> headers,
      final String body) {
    return Result.of(
            () ->
                HttpRequest.builder()
                    .withUrl(url)
                    .withMethod(method)
                    .withHeaders(headers)
                    .withBody(body)
                    .build())
        .flatMap(httpEngine::execute);
  }
}
