package github.com.camilyed.jbolt.domain.workspace;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a specific HTTP request definition.
 * Leaf node in the hierarchy.
 */
public record HttpRequestModel(
        UUID id,
        String name,
        String method,
        String url,
        String body
) implements Resource {
    public HttpRequestModel {
        Objects.requireNonNull(id);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name cannot be empty");
    }

    @Override
    public String getName() {
        return name;
    }
}