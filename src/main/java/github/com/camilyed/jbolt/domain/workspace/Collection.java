package github.com.camilyed.jbolt.domain.workspace;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** A logical collection of resources (Folders or standalone Requests). */
public record Collection(UUID id, String name, List<Resource> items) {
  public Collection {
    Objects.requireNonNull(id);
    items = List.copyOf(items);
  }
}
