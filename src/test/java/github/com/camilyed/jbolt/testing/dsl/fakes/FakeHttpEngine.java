package github.com.camilyed.jbolt.testing.dsl.fakes;

import github.com.camilyed.jbolt.common.result.Result;
import github.com.camilyed.jbolt.domain.execution.HttpEngine;
import github.com.camilyed.jbolt.domain.execution.HttpRequest;
import github.com.camilyed.jbolt.domain.execution.HttpResponse;

public final class FakeHttpEngine implements HttpEngine {

  private Result<HttpResponse> nextResult;
  private HttpRequest lastCapturedRequest;

  public void willReturn(final HttpResponse response) {
    this.nextResult = Result.success(response);
  }

  public void willFail(final Throwable error) {
    this.nextResult = Result.failure(error);
  }

  public HttpRequest lastRequest() {
    return lastCapturedRequest;
  }

  @Override
  public Result<HttpResponse> execute(final HttpRequest request) {
    this.lastCapturedRequest = request;
    if (nextResult == null) {
      throw new IllegalStateException(
          "FakeHttpEngine not configured! Call willReturn() or willFail() first.");
    }
    return nextResult;
  }
}
