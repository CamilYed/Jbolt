package github.com.camilyed.jbolt.domain.workspace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static github.com.camilyed.jbolt.testing.dsl.assertions.WorkspaceAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectWorkspaceServiceTest {

    private final ProjectService service = new ProjectService();

    private static final String VALID_NAME = "My Project";
    private static final String COLLECTION_NAME = "Auth API";
    private static final String FOLDER_NAME = "V1";
    private static final String REQUEST_NAME = "Login";

    @Nested
    @DisplayName("Project Management")
    class ProjectTests {

        @Test
        @DisplayName("should create a new project with correct name and zero collections")
        void shouldCreateProject() {
            // when
            final var project = service.createNewProject(VALID_NAME);

            // then
            assertThat(project)
                    .hasName(VALID_NAME)
                    .hasCollectionsCount(0);
        }

        @Test
        @DisplayName("should add collection to project returning new instance")
        void shouldAddCollection() {
            // given
            final var project = service.createNewProject(VALID_NAME);

            // when
            final var updated = service.addCollection(project, COLLECTION_NAME);

            // then
            assertThat(updated).hasCollectionsCount(1);
            assertThat(project).hasCollectionsCount(0); // Immutability check
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("should throw when creating project with invalid name")
        void shouldValidateProjectName(final String invalidName) {
            assertThrows(IllegalArgumentException.class, () -> service.createNewProject(invalidName));
        }

        @Test
        @DisplayName("should throw when adding collection to null project")
        void shouldThrowOnNullProject() {
            assertThrows(NullPointerException.class, () -> service.addCollection(null, COLLECTION_NAME));
        }
    }

    @Nested
    @DisplayName("Collection Content Management")
    class CollectionTests {

        @Test
        @DisplayName("should add folder to collection")
        void shouldAddFolder() {
            // given
            final var collection = ProjectFactory.createEmptyCollection(COLLECTION_NAME);

            // when
            final var updated = service.addFolderToCollection(collection, FOLDER_NAME);

            // then
            assertThat(updated)
                    .hasItemsCount(1)
                    .containsItem(FOLDER_NAME);
        }

        @Test
        @DisplayName("should add request directly to collection")
        void shouldAddRequest() {
            // given
            final var collection = ProjectFactory.createEmptyCollection(COLLECTION_NAME);

            // when
            final var updated = service.addRequestToCollection(collection, REQUEST_NAME, "GET", "/");

            // then
            assertThat(updated)
                    .hasItemsCount(1)
                    .containsItem(REQUEST_NAME);
        }

        @Test
        @DisplayName("should allow mixing folders and requests in collection")
        void shouldMixItems() {
            // given
            var collection = ProjectFactory.createEmptyCollection(COLLECTION_NAME);
            collection = service.addFolderToCollection(collection, FOLDER_NAME);

            // when
            final var updated = service.addRequestToCollection(collection, REQUEST_NAME, "POST", "/login");

            // then
            assertThat(updated)
                    .hasItemsCount(2)
                    .containsItem(FOLDER_NAME)
                    .containsItem(REQUEST_NAME);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("should validate names when adding items to collection")
        void shouldValidateItemNames(final String invalidName) {
            final var col = ProjectFactory.createEmptyCollection(COLLECTION_NAME);
            assertThrows(IllegalArgumentException.class, () -> service.addFolderToCollection(col, invalidName));
            assertThrows(IllegalArgumentException.class, () -> service.addRequestToCollection(col, invalidName, "GET", "/"));
        }
    }

    @Nested
    @DisplayName("Folder Content Management")
    class FolderTests {

        @Test
        @DisplayName("should add request to folder")
        void shouldAddRequestToFolder() {
            // given
            final var folder = ProjectFactory.createFolder(FOLDER_NAME);

            // when
            final var updated = service.addRequestToFolder(folder, REQUEST_NAME, "PUT", "/update");

            // then
            assertThat(updated).hasRequestsCount(1);
            assertThat(folder).hasRequestsCount(0); // Immutability
        }

        @Test
        @DisplayName("should throw when adding request to null folder")
        void shouldThrowOnNullFolder() {
            assertThrows(NullPointerException.class, () ->
                    service.addRequestToFolder(null, REQUEST_NAME, "GET", "/")
            );
        }
    }

    @Nested
    @DisplayName("Deep Immutability & Safety")
    class ImmutabilityTests {

        @Test
        @DisplayName("should not allow modification of project collections list")
        void shouldLockProjectCollections() {
            // given
            final var project = service.createNewProject(VALID_NAME);

            // when / then
            assertThrows(UnsupportedOperationException.class, () ->
                    project.collections().add(ProjectFactory.createEmptyCollection("Hacker"))
            );
        }

        @Test
        @DisplayName("should not allow modification of collection items list")
        void shouldLockCollectionItems() {
            // given
            final var collection = ProjectFactory.createEmptyCollection(COLLECTION_NAME);

            // when / then
            assertThrows(UnsupportedOperationException.class, () ->
                    collection.items().add(ProjectFactory.createFolder("Hacker"))
            );
        }

        @Test
        @DisplayName("should not allow modification of folder requests list")
        void shouldLockFolderRequests() {
            // given
            final var folder = ProjectFactory.createFolder(FOLDER_NAME);

            // when / then
            assertThrows(UnsupportedOperationException.class, () ->
                    folder.requests().add(ProjectFactory.createRequest("Hack", "GET", "/"))
            );
        }
    }
}