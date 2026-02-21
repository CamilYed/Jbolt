package github.com.camilyed.jbolt.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Result Pattern Tests (100% Coverage)")
class ResultTest {

    // --- FACTORY METHODS ---

    @Test
    @DisplayName("should create Success with valid value")
    void shouldCreateSuccess() {
        // when
        final var result = Result.success("OK");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(((Result.Success<String>) result).value()).isEqualTo("OK");
    }

    @Test
    @DisplayName("should throw NPE when creating Success with null")
    void shouldThrowOnNullSuccess() {
        // expect
        assertThatThrownBy(() -> Result.success(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("should create Failure with valid exception")
    void shouldCreateFailure() {
        // given
        final var error = new RuntimeException("Boom");

        // when
        final var result = Result.failure(error);

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?>) result).error()).isEqualTo(error);
    }

    @Test
    @DisplayName("Result.of() should capture checked IOException")
    void shouldCaptureCheckedException() {
        // given
        final var message = "Disk read error";

        // when
        final Result<String> result = Result.of(() -> {
            throw new java.io.IOException(message); // Checked exception
        });

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result).isInstanceOf(Result.Failure.class);

        final var failure = (Result.Failure<String>) result;
        assertThat(failure.error()).isInstanceOf(java.io.IOException.class);
        assertThat(failure.error().getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Result.of() should return Success when throwing supplier succeeds")
    void shouldReturnSuccessFromThrowingSupplier() {
        // when
        final var result = Result.of(() -> "Success Path");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(((Result.Success<String>) result).value()).isEqualTo("Success Path");
    }

    @Test
    @DisplayName("should throw NPE when creating Failure with null")
    void shouldThrowOnNullFailure() {
        // expect
        assertThatThrownBy(() -> Result.failure(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Result.of() should return Success when supplier succeeds")
    void shouldCreateSuccessFromSupplier() {
        // when
        final var result = Result.of(() -> "Safe");

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(((Result.Success<String>) result).value()).isEqualTo("Safe");
    }

    @Test
    @DisplayName("Result.of() should return Failure when supplier throws exception")
    void shouldCreateFailureFromSupplier() {
        // given
        final var exception = new IllegalStateException("Supplier failed");

        // when
        final var result = Result.of(() -> {
            throw exception;
        });

        // then
        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<Object>) result).error()).isEqualTo(exception);
    }

    // --- TRANSFORMATION: map() ---

    @Test
    @DisplayName("map() should transform value when Success")
    void shouldMapSuccess() {
        // given
        final var result = Result.success(10);

        // when
        final var mapped = result.map(i -> i * 2);

        // then
        assertThat(mapped.isSuccess()).isTrue();
        assertThat(((Result.Success<Integer>) mapped).value()).isEqualTo(20);
    }

    @Test
    @DisplayName("map() should return Failure when mapper throws exception")
    void shouldCatchExceptionInMap() {
        // given
        final var result = Result.success("Input");
        final var exception = new RuntimeException("Mapping error");

        // when
        final var mapped = result.map(s -> {
            throw exception;
        });

        // then
        assertThat(mapped.isSuccess()).isFalse();
        assertThat(((Result.Failure<Object>) mapped).error()).isEqualTo(exception);
    }

    @Test
    @DisplayName("map() should be ignored when result is Failure")
    void shouldSkipMapOnFailure() {
        // given
        final var error = new RuntimeException("Original error");
        final var result = Result.<String>failure(error);

        // when
        final var mapped = result.map(String::toUpperCase);

        // then
        assertThat(mapped.isSuccess()).isFalse();
        assertThat(((Result.Failure<String>) mapped).error()).isEqualTo(error);
        assertThat(mapped).isSameAs(result); // Verify strictly same instance
    }

    // --- CHAINING: flatMap() ---

    @Test
    @DisplayName("flatMap() should chain to new Success")
    void shouldFlatMapSuccess() {
        // given
        final var result = Result.success("123");

        // when
        final var chained = result.flatMap(s -> Result.success(Integer.parseInt(s)));

        // then
        assertThat(chained.isSuccess()).isTrue();
        assertThat(((Result.Success<Integer>) chained).value()).isEqualTo(123);
    }

    @Test
    @DisplayName("flatMap() should chain to Failure")
    void shouldFlatMapToFailure() {
        // given
        final var result = Result.success("123");
        final var error = new RuntimeException("Parsing failed");

        // when
        final var chained = result.flatMap(s -> Result.failure(error));

        // then
        assertThat(chained.isSuccess()).isFalse();
        assertThat(((Result.Failure<Object>) chained).error()).isEqualTo(error);
    }

    @Test
    @DisplayName("flatMap() should catch exception thrown inside mapping function")
    void shouldCatchExceptionInFlatMap() {
        // given
        final var result = Result.success("Input");
        final var exception = new RuntimeException("Crash");

        // when
        final var chained = result.flatMap(s -> {
            throw exception;
        });

        // then
        assertThat(chained.isSuccess()).isFalse();
        assertThat(((Result.Failure<Object>) chained).error()).isEqualTo(exception);
    }

    @Test
    @DisplayName("flatMap() should be ignored when result is Failure")
    void shouldSkipFlatMapOnFailure() {
        // given
        final var error = new RuntimeException("Original error");
        final var result = Result.<String>failure(error);

        // when
        final var chained = result.flatMap(s -> Result.success("New"));

        // then
        assertThat(chained.isSuccess()).isFalse();
        assertThat(((Result.Failure<String>) chained).error()).isEqualTo(error);
        assertThat(chained).isSameAs(result);
    }

    // --- SIDE EFFECTS: onSuccess() ---

    @Test
    @DisplayName("onSuccess() should execute action when Success")
    void onSuccessShouldRun() {
        // given
        final var result = Result.success("Value");
        final var called = new AtomicBoolean(false);

        // when
        result.onSuccess(v -> {
            called.set(true);
            assertThat(v).isEqualTo("Value");
        });

        // then
        assertThat(called).isTrue();
    }

    @Test
    @DisplayName("onSuccess() should NOT execute action when Failure")
    void onSuccessShouldSkip() {
        // given
        final var result = Result.failure(new Exception());
        final var called = new AtomicBoolean(false);

        // when
        result.onSuccess(v -> called.set(true));

        // then
        assertThat(called).isFalse();
    }

    @Test
    @DisplayName("onSuccess() should return 'this' for chaining")
    void onSuccessShouldReturnThis() {
        // given
        final var result = Result.success("Value");

        // when
        final var returned = result.onSuccess(v -> {});

        // then
        assertThat(returned).isSameAs(result);
    }

    // --- SIDE EFFECTS: onFailure() ---

    @Test
    @DisplayName("onFailure() should execute action when Failure")
    void onFailureShouldRun() {
        // given
        final var error = new Exception("Error");
        final var result = Result.failure(error);
        final var captured = new AtomicReference<Throwable>();

        // when
        result.onFailure(captured::set);

        // then
        assertThat(captured.get()).isEqualTo(error);
    }

    @Test
    @DisplayName("onFailure() should NOT execute action when Success")
    void onFailureShouldSkip() {
        // given
        final var result = Result.success("Value");
        final var called = new AtomicBoolean(false);

        // when
        result.onFailure(e -> called.set(true));

        // then
        assertThat(called).isFalse();
    }

    @Test
    @DisplayName("onFailure() should return 'this' for chaining")
    void onFailureShouldReturnThis() {
        // given
        final var result = Result.failure(new Exception());

        // when
        final var returned = result.onFailure(e -> {});

        // then
        assertThat(returned).isSameAs(result);
    }

    // --- BOOLEAN CHECKS ---

    @Test
    @DisplayName("isSuccess() should return correct boolean")
    void isSuccessCheck() {
        assertThat(Result.success(1).isSuccess()).isTrue();
        assertThat(Result.failure(new Exception()).isSuccess()).isFalse();
    }
}