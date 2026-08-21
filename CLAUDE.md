# Engineering guidelines

This file is the working agreement for anyone (human or agent) writing code in this repository.
It is intentionally opinionated. If a change conflicts with something here, either follow this
document or update it deliberately — don't silently drift from it.

See also: [README.md](README.md) for the product pitch, [ROADMAP.md](ROADMAP.md) for what's built
vs. planned, [CONTRIBUTING.md](CONTRIBUTING.md) for the PR checklist.

## Contents

- [Language style](#language-style)
- [Architecture](#architecture)
- [UI architecture: no FXML](#ui-architecture-no-fxml)
- [Testing philosophy](#testing-philosophy)
- [Test building blocks](#test-building-blocks)
- [Coverage](#coverage)
- [Formatting and CI gates](#formatting-and-ci-gates)

## Language style

- **All variables are `final` by default** — local variables, method parameters, and fields.
  Mutability must be an explicit, deliberate choice, not a default. If something needs to change
  after construction, that's a signal to reconsider the design (a new immutable value, a builder,
  a `Property` for JavaFX-bound state) before reaching for a plain mutable field.
- **Prefer records for immutable data, sealed interfaces for closed sets of variants.** The
  workspace domain already does this well — `Project`/`Collection`/`HttpRequestModel`/`Folder` are
  records, and `Resource` is a `sealed interface permits Folder, HttpRequestModel`. `Result<T>`
  (`common.result`) is the same pattern applied to success/failure instead of domain data. New code
  should follow this default rather than reaching for a mutable class or a boolean/enum flag.
- **Default to package-private visibility.** A class, interface, method, or field is `public` only
  when it's genuinely part of a module's public API. `ProjectFactory` is the model to copy:
  package-private, only reachable through `ProjectService`.
- **Javadoc on public API**, describing the contract for a caller, not the implementation.
  Package-private code doesn't need it, though a short summary is welcome when the class's role
  isn't obvious from its name (most classes in this repo already do this — keep it up).
- Clean Code basics apply throughout: small methods, one level of abstraction per method,
  intention-revealing names, no boolean/flag parameters that silently change behavior. If a method
  needs a comment to explain *what* it does, it should probably be split or renamed instead.
- **No `Utils`/`Helper` grab-bag classes.** If you're reaching for `XyzUtils`, stop and ask what the
  method is actually the logic *of*, and give it that home instead.
- Prefer `java.util.Optional`, `Result<T>`, or an explicit "not present" domain object over `null`
  as a silent default, especially at module boundaries (`domain` ↔ `application` ↔
  `infrastructure` ↔ `ui`).

## Architecture

Layering, roughly Ports & Adapters (Hexagonal):

- **`domain`** owns business rules and has no framework dependencies — no JavaFX, no
  `java.net.http`, no Jackson. `domain.execution` defines the `HttpEngine` port ("execute this
  request, get back a response or throw") independent of any HTTP client; `domain.workspace` is
  the collection/folder/request aggregate model, framework-free by construction (plain records).
- **`infrastructure`** provides adapters for domain ports. `JavaNetHttpEngine` is the current
  `HttpEngine` adapter, built on `java.net.http`. A future alternate transport (e.g. one with
  connection-pool tuning for Phase 4's stress testing) is a second adapter behind the same port —
  `application`/`ui` should never need to change to add one.
- **`application`** orchestrates domain + ports for a specific use case — `RequestExecutionService`
  is the one example today: it turns UI-level parameters into a domain `HttpRequest`, executes it
  through the `HttpEngine` port, and returns a `Result`. As the workspace gets wired to the UI
  (Roadmap Phase 1), collection/environment use cases belong here too, not in `ui` or `domain`
  directly — `ui` should stay thin (build views, bind to view models), `domain` should stay pure.
- **`ui`** builds and binds views. See the dedicated section below for how views are built.
- A module should not reach into another module's internals — if `ui` needs something from
  `domain`, it goes through `application` or a small port, not a package-private class.
- Use DDD tactical patterns where domain complexity actually warrants it (the workspace aggregate
  already does — `Project` as an aggregate root, `ProjectService` enforcing invariants like
  non-blank names). Don't force DDD ceremony onto code with no real domain logic to protect.

## UI architecture: no FXML

JBolt does not use FXML, Scene Builder, or `@FXML` injection. Views are built in plain Java by
implementing `Component<T extends Region>`:

```java
public interface Component<T extends Region> {
    T build();
}
```

A `Component` implementation constructs its node tree, wires bindings to a view model, and returns
the root region from `build()`. See `MainController` and `RequestTabController` for the pattern in
practice — both are `Component`s that build their tree in `build()` and expose no `@FXML` fields.

Why: FXML/`@FXML` injection defers wiring errors to runtime (a typo in `fx:id` fails when the file
loads, not when the project compiles), splits one logical view across two files (markup +
controller) that must be kept in sync by hand, and needs `opens ... to javafx.fxml` reflection
exceptions in `module-info.java`. Plain Java construction is compiler-checked, is one file per
view, and needs no reflective module opens. This mirrors the direction the JavaFX community's own
FXML-free tooling (e.g. XPipe/KickstartFX's `Comp`-style builders) has moved for exactly these
reasons — JBolt's `Component` is a deliberately small version of the same idea, without pulling in
extra dependencies before the app actually needs their extra power (fluent `apply`/augment chains,
automatic property-lifecycle binding helpers). Revisit that tradeoff if view code starts repeating
itself enough to justify a small builder-utility layer on top of `Component`.

Conventions:

- One `Component` per logical view; private `buildXyz()` methods for sub-trees, not one giant
  `build()`.
- Any node a test needs to locate gets `setId(...)` explicitly (FXML's automatic `fx:id` → `id`
  behavior doesn't exist without FXML) — see `RequestTabControllerTest`'s use of
  `root.lookup("#urlField")`.
- Keep `Component`s dumb about *where data comes from* — they bind to a view model
  (`RequestTabViewModel`) or take pre-built collaborators through the constructor
  (`MainController`'s `ViewLoader`), never reach into `application`/`domain` services directly.

## Testing philosophy

This project follows the black-box, no-Mockito testing style described in:

- [Tests That Don't Lie, Part 1: Readability and DSL](https://camilyed.github.io/en/tests-that-dont-lie/)
- [Tests That Don't Lie, Part 2: The Mockito Trap and In-Memory Implementations](https://camilyed.github.io/en/tests-that-dont-lie-part-2/)

The short version: a test should tell you *what* the system does, not *how* it does it internally,
and it should fail only when actual behavior changes — never because of a harmless refactor.

### Development workflow: TDD

Red-green-refactor, always:

1. **Red.** Write one failing test expressing the behavior you're about to add. Run it, confirm it
   fails for the right reason.
2. **Green.** Write the smallest amount of production code that makes it pass.
3. **Refactor.** With the test green, clean up without changing behavior. Re-run after every step.

Skeleton/plumbing code with no behavior yet (a module's `build.gradle.kts`, `module-info.java`) is
the explicit exception — there's nothing to red-green there.

### Hard rules

1. **No Mockito. Ever.** `verify(...)`/`when(...)` tests interaction with a library call, not
   business outcome. `WireMock` (a real embedded HTTP server) and TestFX (a real UI-automation
   robot) are not exceptions to this rule — they exercise real behavior end-to-end rather than
   stubbing interactions, which is exactly the point.
2. **Black-box over white-box.** Assert on resulting *state*, not on which internal method got
   called how many times.
3. **In-memory fakes instead of mocks.** `FakeHttpEngine` and `FakeUiMessageService`
   (`testing.dsl.fakes`) are the model: small real implementations of the actual interface, not
   mocks fed with `when(...).thenReturn(...)`.
4. **Given-When-Then structure, explicit comments.** `// given`, `// when`, `// then`, `// and` for
   grouped sub-steps. No exceptions for small tests.
5. **No shared mutable variables between `given` and `then`.** Use literals directly in both places.
6. **No loops or conditionals inside a test body.** Push iteration into AssertJ (`extracting`,
   `filteredOn`, `containsExactly`) or a custom assertion.
7. **Isolation is mandatory.** Every test starts from a clean, known state; no test depends on
   execution order or another test's leftover state.
8. **No `Thread.sleep` in tests. Use Awaitility** for anything asynchronous (`RequestTabViewModel`'s
   `CompletableFuture`-based `sendRequest()` is exactly this case — see
   `RequestTabViewModelTest`'s `await().atMost(...).untilAsserted(...)` usage).
9. **JavaFX toolkit tests start it once, defensively.** `Platform.startup(() -> {})` wrapped in a
   `try { ... } catch (IllegalStateException _) {}` in `@BeforeAll` — see any of the `ui` test
   classes for the pattern. Never assume the toolkit is already running or start it more than once
   per JVM.

## Test building blocks

Use Test Data Builders, the Ability pattern, and custom AssertJ assertions together — see
`testing.dsl` (`DomainDSL`, `WorkspaceDSL`, `JsonTestDataBuilder`,
`assertions.ResultAssertion`/`WorkspaceAssertions`, `fakes.FakeHttpEngine`/`FakeUiMessageService`)
for this project's own examples of each. Test Data Builders, Abilities, custom assertions, and
fakes each get their own sub-package (`builders`, `abilities`, `assertions`, `fakes`) rather than
sitting loose next to the tests that use them.

## Coverage

JaCoCo runs after every test task; SonarCloud enforces its default **new-code coverage gate**
(≥ 80% on changed lines) rather than a blind 100% line-coverage target. Chasing 100% forces tests
onto code with no behavior to protect (a record's generated `equals`/`hashCode`, `module-info.java`)
and directly contradicts the black-box rule above ("assert on what the system does", not "make the
number go up"). Treat the JaCoCo HTML report as a routine "did I miss an obvious branch" check
during development, and treat a module sitting far below its neighbors as a signal to go look — not
an automatic failure.

## Formatting and CI gates

- **Spotless** (Google Java Format) — `./gradlew spotlessCheck` / `./gradlew spotlessApply`.
- **JaCoCo** — coverage report after every test run, feeding the SonarCloud properties in the root
  build file.
- **SonarQube/SonarCloud** — `./gradlew sonar` (CI runs this when `SONAR_TOKEN` is set).
- CI (`.github/workflows/ci.yml`) runs `spotlessCheck`, `clean build`, `jacocoTestReport`, and the
  Sonar analysis on every push/PR.

Before opening a PR: `./gradlew spotlessCheck clean build` must pass locally.
