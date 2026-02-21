package github.com.camilyed.jbolt.ui;

import javafx.util.Callback;

/**
 * Functional interface for providing JavaFX controller instances with injected dependencies.
 */
@FunctionalInterface
public interface ControllerFactory extends Callback<Class<?>, Object> {
}