# Agent instructions

See [CLAUDE.md](CLAUDE.md) for the full engineering guidelines (language style, architecture, the
no-FXML UI pattern, testing philosophy, coverage, CI gates). This project has a single source of
truth for those rules to avoid `AGENTS.md`/`CLAUDE.md` drifting apart — read `CLAUDE.md` before
making changes. See [ROADMAP.md](ROADMAP.md) for what's built vs. planned before proposing new
features or architecture changes.

Quick reminders most relevant to autonomous changes:

- `final` everywhere (locals, params, fields) unless there's a real reason not to.
- No FXML. Build views by implementing `Component<T extends Region>` — see `MainController` /
  `RequestTabController`. Any node a test needs to find gets `setId(...)` explicitly.
- No Mockito, ever. Black-box tests with in-memory fakes (`testing.dsl.fakes`), Given-When-Then,
  Test Data Builders, the Ability pattern, custom AssertJ assertions.
- TDD: red-green-refactor. Write the failing test before the production code, except for pure
  skeleton/plumbing with no behavior (a `build.gradle.kts`, `module-info.java`).
- `./gradlew spotlessCheck clean build` must pass before considering a change done.
- Check [ROADMAP.md](ROADMAP.md) for phase ordering before adding a big feature out of order —
  e.g. don't start stress testing (Phase 4) before the workspace/persistence work (Phase 1) it
  depends on.
