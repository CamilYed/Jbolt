package github.com.camilyed.jbolt.testing.dsl;

import github.com.camilyed.jbolt.domain.workspace.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WorkspaceDSL {

    public static final class ProjectBuilder {
        private String name = "Default Project";
        private final List<Collection> collections = new ArrayList<>();

        public static ProjectBuilder aProject() {
            return new ProjectBuilder();
        }

        public ProjectBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public ProjectBuilder withCollection(final Collection collection) {
            this.collections.add(collection);
            return this;
        }

        public Project build() {
            return new Project(UUID.randomUUID(), name, List.copyOf(collections));
        }
    }

    public static final class CollectionBuilder {
        private String name = "Default Collection";
        private final List<Resource> items = new ArrayList<>();

        public static CollectionBuilder aCollection() {
            return new CollectionBuilder();
        }

        public CollectionBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public CollectionBuilder withRequest(final HttpRequestModel request) {
            this.items.add(request);
            return this;
        }

        public CollectionBuilder withFolder(final Folder folder) {
            this.items.add(folder);
            return this;
        }

        public Collection build() {
            return new Collection(UUID.randomUUID(), name, List.copyOf(items));
        }
    }

    public static final class FolderBuilder {
        private String name = "Default Folder";
        private final List<HttpRequestModel> requests = new ArrayList<>();

        public static FolderBuilder aFolder() {
            return new FolderBuilder();
        }

        public FolderBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public FolderBuilder withRequest(final HttpRequestModel request) {
            this.requests.add(request);
            return this;
        }

        public Folder build() {
            return new Folder(name, List.copyOf(requests));
        }
    }

    public static final class HttpRequestBuilder {
        private String name = "Default Request";
        private String method = "GET";
        private String url = "{{baseUrl}}";

        public static HttpRequestBuilder aRequestBuilder() {
            return new HttpRequestBuilder();
        }

        public HttpRequestBuilder withName(final String name) {
            this.name = name;
            return this;
        }

        public HttpRequestBuilder withMethod(final String method) {
            this.method = method;
            return this;
        }

        public HttpRequestModel build() {
            return new HttpRequestModel(UUID.randomUUID(), name, method, url, "");
        }
    }
}