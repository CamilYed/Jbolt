package github.com.camilyed.jbolt.domain.workspace;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The Root Aggregate of the workspace domain.
 */
public record Project(
        UUID id,
        String name,
        List<Collection> collections
) {
    public Project {
        Objects.requireNonNull(id);
        collections = List.copyOf(collections);
    }
}