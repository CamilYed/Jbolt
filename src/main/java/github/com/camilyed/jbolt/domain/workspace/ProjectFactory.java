package github.com.camilyed.jbolt.domain.workspace;

import java.util.List;
import java.util.UUID;

/**
 * Internal factory for creating workspace entities. Not accessible outside the package to enforce
 * service usage.
 */
final class ProjectFactory {

  private ProjectFactory() {}

  static Project createNewProject(final String name) {
    return new Project(UUID.randomUUID(), name, List.of());
  }

  static Collection createEmptyCollection(final String name) {
    return new Collection(UUID.randomUUID(), name, List.of());
  }

  static Folder createFolder(final String name) {
    return new Folder(name, List.of());
  }

  static HttpRequestModel createRequest(final String name, final String method, final String url) {
    return new HttpRequestModel(UUID.randomUUID(), name, method, url, "");
  }
}
