package github.com.camilyed.jbolt.domain.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Primary entry point for managing workspace domain logic. Ensures all business invariants are met
 * before structural changes.
 */
public final class ProjectService {

  public Project createNewProject(final String name) {
    validateName(name);
    return ProjectFactory.createNewProject(name);
  }

  public Project addCollection(final Project project, final String collectionName) {
    Objects.requireNonNull(project);
    validateName(collectionName);

    final var newCollection = ProjectFactory.createEmptyCollection(collectionName);
    final var collections = new ArrayList<>(project.collections());
    collections.add(newCollection);

    return new Project(project.id(), project.name(), List.copyOf(collections));
  }

  public Collection addFolderToCollection(final Collection collection, final String folderName) {
    Objects.requireNonNull(collection);
    validateName(folderName);

    final var folder = ProjectFactory.createFolder(folderName);
    final var items = new ArrayList<>(collection.items());
    items.add(folder);

    return new Collection(collection.id(), collection.name(), List.copyOf(items));
  }

  public Collection addRequestToCollection(
      final Collection collection, final String name, final String method, final String url) {
    Objects.requireNonNull(collection);
    validateName(name);

    final var request = ProjectFactory.createRequest(name, method, url);
    final var items = new ArrayList<>(collection.items());
    items.add(request);

    return new Collection(collection.id(), collection.name(), List.copyOf(items));
  }

  public Folder addRequestToFolder(
      final Folder folder, final String name, final String method, final String url) {
    Objects.requireNonNull(folder);
    validateName(name);

    final var request = ProjectFactory.createRequest(name, method, url);
    final var requests = new ArrayList<>(folder.requests());
    requests.add(request);

    return new Folder(folder.name(), List.copyOf(requests));
  }

  private void validateName(final String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name cannot be blank");
    }
  }
}
