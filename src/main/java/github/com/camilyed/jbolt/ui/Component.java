package github.com.camilyed.jbolt.ui;

import javafx.scene.layout.Region;

/**
 * A UI building block that assembles a JavaFX {@link Region} tree in plain Java code.
 *
 * <p>JBolt does not use FXML: every view is built by a {@code Component} implementation that
 * constructs its nodes, wires bindings to a view model (if any), and returns the resulting root
 * region from {@link #build()}. This keeps view construction type-safe, refactor-friendly, and free
 * of the loader/controller-factory indirection FXML requires.
 *
 * @param <T> the concrete root {@link Region} type this component builds
 */
public interface Component<T extends Region> {

  /**
   * Builds and returns this component's root region. Implementations construct a fresh node tree on
   * every call; callers own the returned instance.
   */
  T build();
}
