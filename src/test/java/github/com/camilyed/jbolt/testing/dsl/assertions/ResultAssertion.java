package github.com.camilyed.jbolt.testing.dsl.assertions;

import github.com.camilyed.jbolt.common.result.Result;
import java.util.function.Consumer;
import org.assertj.core.api.AbstractAssert;

public final class ResultAssertion<T> extends AbstractAssert<ResultAssertion<T>, Result<T>> {

  private ResultAssertion(Result<T> actual) {
    super(actual, ResultAssertion.class);
  }

  public static <T> ResultAssertion<T> assertThatResult(Result<T> actual) {
    return new ResultAssertion<>(actual);
  }

  public ResultAssertion<T> isSuccess() {
    isNotNull();
    if (!actual.isSuccess()) {
      if (actual instanceof Result.Failure<T> fail) {
        failWithMessage(
            "Expected result to be Success but was Failure with error: <%s>",
            fail.error().getMessage());
      } else {
        failWithMessage("Expected result to be Success but was Failure");
      }
    }
    return this;
  }

  /** Allows drilling down into the success value with lambda assertions. */
  public ResultAssertion<T> isSuccess(Consumer<T> valueRequirements) {
    isSuccess();
    if (actual instanceof Result.Success<T>(T value)) {
      valueRequirements.accept(value);
    }
    return this;
  }

  public ResultAssertion<T> isFailure() {
    isNotNull();
    if (actual.isSuccess()) {
      failWithMessage(
          "Expected result to be Failure but was Success with value: <%s>",
          ((Result.Success<T>) actual).value());
    }
    return this;
  }

  public ResultAssertion<T> isFailure(Consumer<Throwable> errorRequirements) {
    isFailure();
    if (actual instanceof Result.Failure<T>(Throwable error)) {
      errorRequirements.accept(error);
    }
    return this;
  }
}
