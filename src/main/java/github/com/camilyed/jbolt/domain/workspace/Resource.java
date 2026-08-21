package github.com.camilyed.jbolt.domain.workspace;

/**
 * A sealed interface representing a leaf or a simple container in the workspace. To maintain a
 * shallow hierarchy, it only allows Folder or HttpRequestModel.
 */
public sealed interface Resource permits Folder, HttpRequestModel {
  String getName();
}
