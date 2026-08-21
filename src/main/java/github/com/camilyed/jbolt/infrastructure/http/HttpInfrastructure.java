package github.com.camilyed.jbolt.infrastructure.http;

import github.com.camilyed.jbolt.domain.execution.HttpEngine;

/**
 * Public entry point for the HTTP infrastructure module. Provides the domain with the required port
 * implementations while keeping adapters hidden.
 */
public final class HttpInfrastructure {

  private HttpInfrastructure() {}

  public static HttpEngine defaultEngine() {
    return new JavaNetHttpEngine();
  }
}
