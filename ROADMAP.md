# JBolt Roadmap

## Vision

JBolt is a native, offline-first REST API client and stress-testing tool: a free (and later,
optionally paid) replacement for Postman aimed at developers who want a fast, low-memory,
privacy-respecting desktop tool instead of an Electron app phoning home to a cloud account.

The wedge that makes JBolt worth building instead of "just another Postman clone":

- **Virtual-thread-powered load testing built in, not bolted on.** Most Postman alternatives treat
  load/stress testing as an afterthought (a paid add-on, a separate CLI tool, or absent entirely).
  JBolt runs on Java 25 + Project Loom, so simulating thousands of concurrent requests from a
  desktop app doesn't require an army of OS threads or a separate k6/Gatling setup — that's a real,
  defensible technical edge, not just a UI reskin.
- **Native JVM app, not Electron.** Lower idle memory, faster startup, a UI that follows the OS
  theme via AtlantaFX instead of shipping a bundled Chromium.
- **Privacy-first by default.** 100% offline, no forced account, no telemetry — collections live on
  disk as files the user owns, not in someone else's cloud.
- **Git-friendly collection storage.** Bruno proved this is a real differentiator against Postman's
  proprietary cloud format: plain, diffable files that work naturally with `git diff`/`git blame`
  and PR review, no export/import dance required to version a collection.

## Where things stand today

Single Gradle module (`github.com.camilyed.jbolt`), layered `domain` / `application` /
`infrastructure` / `ui`, JavaFX 25 + AtlantaFX for UI (no FXML — see
[CLAUDE.md](CLAUDE.md#ui-architecture-no-fxml)).

| Area | State |
| --- | --- |
| HTTP execution | Working: `HttpEngine` port + `JavaNetHttpEngine` adapter (`java.net.http`), wired through `RequestExecutionService`. Single request at a time, no cookies/redirect policy exposed, no per-request timeout configuration yet. |
| Request tab UI | Working: method/URL/send bar, JSON-pretty-printed response, status/time, multi-tab. Headers editor is a placeholder ("coming soon"). No query-params editor, no auth tab, no pre/post-request scripting. |
| Workspace domain model | Modeled but **not wired to the UI or persisted**: `Project` → `Collection` → `Folder`/`HttpRequestModel` as immutable records, `ProjectService` for structural edits. The sidebar `TreeView` is currently an empty placeholder with no binding to this model at all. |
| Persistence | None. Nothing survives an app restart. |
| Stress testing | Not started — the README's headline feature doesn't exist in code yet. |
| Postman import/export | Not started. |
| Environments/variables | Not started. |

This gap between the README's pitch and the current code is the honest starting point for
phase ordering below: prove out the core request/collection loop and persistence first, since
stress testing and import/export both need a real collection model underneath them to be useful.

## Phase 0 — Foundations (in progress)

- [x] Gradle version catalog (`gradle/libs.versions.toml`), `settings.gradle.kts`,
  Spotless/JaCoCo/Sonar wiring aligned with the team's other projects.
- [x] Remove FXML entirely; UI built via the `Component` pattern (see CLAUDE.md).
- [ ] CI gate on `spotlessCheck` (was missing — build passed even with unformatted code).
- [ ] `AGENTS.md`/`CLAUDE.md` codifying TDD + coverage practice (this change).

## Phase 1 — Make the workspace real

The sidebar has to stop being a placeholder before anything else matters.

1. **Wire `ProjectService`'s domain model into `collectionTree`.** New collection / new folder /
   new request from the UI, reflected live. This is the first real use of the `Component` pattern
   for a dynamic, data-driven tree (as opposed to the static request-tab layout).
2. **File-based persistence**, git-friendly by construction: one readable file per collection
   (JSON to start — Jackson is already a dependency), not one opaque blob for the whole workspace.
   Autosave on change; explicit "Open workspace folder" like an IDE project, not a database file.
3. **Wire "New Request" tabs to the tree** instead of a disconnected "+" tab: opening a tree item
   opens/focuses its tab; closing/saving a tab updates the underlying `HttpRequestModel`.
4. **Headers editor** (currently a placeholder) and a **query-params editor** that stays in sync
   with the URL field (edit either, the other updates — this is the single most-used Postman
   affordance after the URL bar itself).

## Phase 2 — Environments, variables, auth

1. **Environments**: named sets of key/value variables (`{{baseUrl}}` style interpolation into
   URL/headers/body), an environment switcher in the toolbar. This is table-stakes — every
   competitor has it, and collections are far less reusable without it.
2. **Auth tab per request/folder/collection** with inheritance (a request without explicit auth
   inherits from its folder, then its collection): Bearer token, Basic, API Key (header or query
   param) first; OAuth2 (client credentials + auth-code with a local-redirect listener) once the
   simpler flows are solid.
3. **Secret handling**: variables markable as "secret" — masked in the UI, still plaintext on disk
   for now with a clear warning (a proper OS-keychain-backed secret store is a Phase 6+ concern,
   not a blocker for an offline-first free tool).

## Phase 3 — Scripting and assertions

1. **Pre-request and post-response scripts.** Postman/Insomnia both use a JS sandbox
   (Postman: a Node-ish sandbox; Insomnia: similar). For JBolt, evaluate the JDK's built-in
   `jdk.nashorn`-successor situation carefully — Nashorn is removed since JDK 15, so this means
   either embedding GraalJS (`org.graalvm.js:js`) or scoping v1 scripting to a small, purpose-built
   DSL (set/get variable, basic assertions) instead of full JS. Recommendation: ship the narrow DSL
   first, revisit GraalJS only if users actually ask for real JS.
2. **Assertions / a "Tests" tab** per request (status code, header presence, JSON-path value
   checks) with pass/fail shown per tab and rolled up per collection run.
3. **Collection runner**: run every request in a collection (or folder) in sequence, using the
   active environment, showing a pass/fail summary — this is what turns JBolt from "manual request
   tool" into something usable in CI (a `jbolt-cli` companion, Newman-style, is a natural Phase 5+
   follow-on once the runner exists as a library, not just a UI feature).

## Phase 4 — Stress testing (the differentiator)

This is the feature the README already promises and the one no mainstream competitor does well
natively. Don't start it before Phases 1–3 give it a real request/collection/environment model to
drive load from — a load generator with no reusable request definitions is just `hey`/`wrk` with
extra steps.

1. **Load profile UI**: pick a request (or a whole collection/folder), concurrency (virtual
   threads), duration or request-count target, ramp-up.
2. **Live metrics while running**: requests/sec, p50/p95/p99 latency, error rate — updated on a
   throttle (not per-request) so the UI stays responsive under thousands of concurrent virtual
   threads. `HdrHistogram` (already known to the team from the clickhouse-r2dbc-reactive project)
   is the right tool for the latency percentiles.
3. **Result report**: exportable summary (JSON/HTML) of a run — this is also the first genuinely
   "pro tier" candidate feature (see Monetization below): the live view free, a saved/exportable
   historical report gated.
4. **Backpressure and honesty**: make clear in the UI when the *client* (not the target server) is
   the bottleneck (CPU-bound JSON parsing, GC pauses) — a load tester that quietly under-reports
   its own overhead is worse than no load tester.

## Phase 5 — Interop

1. **Postman collection import/export (v2.1)** — the README already promises this; build it once
   the internal `Collection`/`HttpRequestModel` shape has stabilized past Phase 2's auth/env work,
   or the mapping has to be redone.
2. **OpenAPI import**: generate a collection skeleton from an OpenAPI/Swagger spec — high value,
   commonly requested, and a natural fit given `jackson-databind` is already a dependency.
3. **cURL import/export**: paste a cURL command → request; copy a request → cURL. Cheap to build,
   disproportionately useful for pasting from docs/Slack.

## Phase 6 — Polish, protocols, packaging

1. **GraphQL support**: schema introspection, variables panel, syntax highlighting — Insomnia's
   bar to clear, per the competitive research below.
2. **WebSocket / SSE support** — Hoppscotch and Insomnia both cover these; increasingly expected.
3. **UI polish pass**: consistent AtlantaFX theming light/dark, command palette (Cmd/Ctrl+K),
   keyboard-first navigation between tabs, response syntax highlighting (JSON/XML/HTML), response
   size/time budget warnings for huge bodies.
4. **Packaging**: `jlink`/`jpackage` native installers per OS (the `org.beryx.jlink` plugin is
   already in the build) — signed `.dmg`/`.msi`/`.deb` so "download and run" is a real free-tier
   experience, not "clone and `./gradlew run`".

## Phase 7 — Monetization (once the free product is genuinely good)

Do not gate anything that would make the *free* tool feel crippled compared to free competitors —
that backfires (see Insomnia's community reaction to feature-gating history). Plausible paid-tier
candidates, all additive rather than restrictive:

- Saved/exportable stress-test historical reports and trend comparison across runs.
- Team workspace sync (opt-in, since "100% offline, no accounts" is a stated principle for the
  free tier — sync has to be an explicit opt-in add-on, not the default).
- Mock servers (Postman's flagship paid-adjacent feature).
- Priority support / early builds.

## Explicit non-goals for now

- No forced cloud account for the free tier — ever. Sync is opt-in, later, paid.
- No telemetry by default at any tier.
- No Mockito-driven "fake it till you make it" testing — see CLAUDE.md; this is a product
  principle bleeding into an engineering one on purpose, since a tool whose own tests don't prove
  real behavior isn't one users should trust to prove *their* API's real behavior either.

## Competitive notes (informing the above)

- **Bruno**: plaintext `.bru` files stored directly in the user's own Git repo — the single
  biggest lesson to copy (Phase 1's persistence design), because Postman's proprietary cloud
  format is the #1 complaint driving people to alternatives.
- **Insomnia**: best-in-class GraphQL support (schema introspection, autocomplete) — Phase 6 target.
- **Postman**: mock servers, monitors, team workspaces, the biggest plugin/ecosystem surface —
  Postman's own complexity/pricing creep is exactly what's pushing users toward the alternatives
  above, so JBolt should not chase full feature parity, only the subset that's genuinely useful.
- **Thunder Client**: proof that "lightweight and fast" alone is a viable pitch, not just a
  consolation prize — reinforces that JBolt's native/low-memory story matters and shouldn't be
  diluted by feature bloat.
- **Hoppscotch**: broad protocol coverage (GraphQL, WebSocket, SSE, MQTT, Socket.IO) as a web-first
  tool — the protocol breadth is worth matching over time (Phase 6); the "web-first" part is not
  something JBolt should chase, since native desktop is the whole point.
