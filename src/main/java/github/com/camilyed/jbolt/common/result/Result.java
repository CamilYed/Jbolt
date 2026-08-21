package github.com.camilyed.jbolt.common.result;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** A sealed generic result type representing either Success (value) or Failure (error). */
public sealed interface Result<T> permits Result.Success, Result.Failure {

  @FunctionalInterface
  interface ThrowingSupplier<T> {
    T get() throws Exception;
  }

  static <T> Result<T> of(ThrowingSupplier<T> supplier) {
    try {
      return Result.success(supplier.get());
    } catch (Throwable throwable) {
      return Result.failure(throwable);
    }
  }

  static <T> Result<T> success(final T value) {
    return new Success<>(value);
  }

  static <T> Result<T> failure(final Throwable error) {
    return new Failure<>(error);
  }

  // --- Records ---

  record Success<T>(T value) implements Result<T> {
    public Success {
      Objects.requireNonNull(value);
    }
  }

  record Failure<T>(Throwable error) implements Result<T> {
    public Failure {
      Objects.requireNonNull(error);
    }
  }

  // --- Fluent API ---

  default boolean isSuccess() {
    return this instanceof Success;
  }

  /** Maps the successful value to a new value. */
  default <R> Result<R> map(final Function<T, R> mapper) {
    if (this instanceof Success<T>(T value)) {
      try {
        return success(mapper.apply(value));
      } catch (Throwable t) {
        return failure(t);
      }
    } else {
      //noinspection unchecked
      return (Result<R>) this;
    }
  }

  /**
   * Maps the successful value to a new Result (chaining operations). This is crucial for linking
   * Request Builder -> Http Engine.
   */
  default <R> Result<R> flatMap(final Function<T, Result<R>> mapper) {
    if (this instanceof Success<T>(T value)) {
      try {
        return mapper.apply(value);
      } catch (Throwable t) {
        return failure(t);
      }
    } else {
      //noinspection unchecked
      return (Result<R>) this;
    }
  }

  /** Executes side-effect if success (e.g. logging). */
  default Result<T> onSuccess(Consumer<T> action) {
    if (this instanceof Success<T>(T value)) {
      action.accept(value);
    }
    return this;
  }

  /** Executes side-effect if failure. */
  default Result<T> onFailure(Consumer<Throwable> action) {
    if (this instanceof Failure<T>(Throwable error)) {
      action.accept(error);
    }
    return this;
  }
}
