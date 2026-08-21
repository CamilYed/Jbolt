package github.com.camilyed.jbolt.ui.service;

import github.com.camilyed.jbolt.common.result.Result;
import github.com.camilyed.jbolt.ui.Component;
import java.util.function.Supplier;
import javafx.scene.Parent;
import javafx.scene.layout.Region;

/**
 * A {@link ViewLoader} that builds a view by invoking a {@link Component} factory, rather than
 * loading FXML. The {@code viewId} argument is accepted for symmetry with {@link ViewLoader} and
 * for future use once more than one dynamically-created view exists, but a single factory is all
 * that's needed today.
 */
public final class ComponentViewLoader implements ViewLoader {

  private final Supplier<? extends Component<? extends Region>> componentFactory;

  public ComponentViewLoader(
      final Supplier<? extends Component<? extends Region>> componentFactory) {
    this.componentFactory = componentFactory;
  }

  @Override
  public Result<Parent> load(final String viewId) {
    return Result.of(() -> componentFactory.get().build());
  }
}
