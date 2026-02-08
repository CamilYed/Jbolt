package github.com.camilyed.jbolt.domain.execution;

/**
 * The Domain Port (Interface) for the HTTP execution engine.
 * Implementations of this interface should reside in the infrastructure layer.
 */
public interface HttpEngine {
    /**
     * Executes the given domain request and returns a domain response.
     *
     * @param request The domain model containing request details.
     * @return The resulting HttpResponse domain model.
     * @throws Exception if a networking or protocol error occurs during execution.
     */
    HttpResponse execute(HttpRequest request) throws Exception;
}