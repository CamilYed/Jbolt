package github.com.camilyed.jbolt.testing.dsl.assertions;

import github.com.camilyed.jbolt.domain.workspace.Collection;
import github.com.camilyed.jbolt.domain.workspace.Folder;
import github.com.camilyed.jbolt.domain.workspace.Project;
import org.assertj.core.api.AbstractAssert;

public final class WorkspaceAssertions {

    public static ProjectAssert assertThat(final Project actual) {
        return new ProjectAssert(actual);
    }

    public static CollectionAssert assertThat(final Collection actual) {
        return new CollectionAssert(actual);
    }

    public static FolderAssert assertThat(final Folder actual) {
        return new FolderAssert(actual);
    }

    // --- ASSERTIONS CLASSES ---

    public static final class ProjectAssert extends AbstractAssert<ProjectAssert, Project> {
        ProjectAssert(final Project actual) { super(actual, ProjectAssert.class); }

        public ProjectAssert hasName(final String name) {
            if (!actual.name().equals(name)) failWithMessage("Expected project name <%s> but was <%s>", name, actual.name());
            return this;
        }

        public ProjectAssert hasCollectionsCount(final int count) {
            if (actual.collections().size() != count) failWithMessage("Expected <%d> collections but found <%d>", count, actual.collections().size());
            return this;
        }
    }

    public static final class CollectionAssert extends AbstractAssert<CollectionAssert, Collection> {
        CollectionAssert(final Collection actual) { super(actual, CollectionAssert.class); }

        public CollectionAssert hasItemsCount(final int count) {
            if (actual.items().size() != count) failWithMessage("Expected <%d> items in collection but found <%d>", count, actual.items().size());
            return this;
        }

        public CollectionAssert containsItem(final String name) {
            final var found = actual.items().stream().anyMatch(r -> r.getName().equals(name));
            if (!found) failWithMessage("Collection does not contain item <%s>", name);
            return this;
        }
    }

    public static final class FolderAssert extends AbstractAssert<FolderAssert, Folder> {
        FolderAssert(final Folder actual) { super(actual, FolderAssert.class); }

        public FolderAssert hasRequestsCount(final int count) {
            if (actual.requests().size() != count) failWithMessage("Expected <%d> requests in folder but found <%d>", count, actual.requests().size());
            return this;
        }
    }
}