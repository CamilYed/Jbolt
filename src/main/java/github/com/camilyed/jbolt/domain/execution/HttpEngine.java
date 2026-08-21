package github.com.camilyed.jbolt.domain.execution;

import github.com.camilyed.jbolt.common.result.Result;

/**
 * The Domain Port (Interface) for the HTTP execution engine. Implementations of this interface
 * reside in the infrastructure layer.
 */
public interface HttpEngine {

  /**
   * Executes the given domain request safely using the Result pattern.
   *
   * @param request The domain model containing request details.
   * @return A Result containing either the HttpResponse or an execution error.
   */
  Result<HttpResponse> execute(HttpRequest request);
}
