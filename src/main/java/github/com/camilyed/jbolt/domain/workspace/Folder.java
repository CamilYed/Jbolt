package github.com.camilyed.jbolt.domain.workspace;

import java.util.List;

/**
 * A container that can ONLY hold requests, preventing infinite recursion.
 */
public record Folder(
        String name,
        List<HttpRequestModel> requests
) implements Resource {
    public Folder {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Folder name cannot be empty");
        }
        requests = List.copyOf(requests);
    }

    @Override
    public String getName() {
        return name;
    }
}