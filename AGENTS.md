# AGENTS.md

Guidance for coding agents working in `mobie-viewer-fiji`.

## Scope and goals

- Repository: `mobie-viewer-fiji` (single Maven module).
- Main purpose: Fiji plugin for opening and exploring MoBIE projects and related image/table inputs.
- Priority: keep behavior stable for Fiji users; prefer minimal, targeted changes.

## Project map

- `src/main/java/org/embl/mobie/`
  - `MoBIE.java`: central startup/orchestration class.
  - `MoBIESettings.java`: runtime settings flow.
  - `cmd/`: CLI entry points (`ProjectCmd`, `FilesCmd`, `TableCmd`, `HCSCmd`) using picocli.
  - `command/`, `lib/`, `plugins/`, `ui/`: core features and UI integration.
- `src/test/java/`
  - Mix of JUnit tests and integration-style tests; several tests interact with ImageJ/Fiji GUI behavior.
- `src/main/resources/`
  - App assets and packaged binaries (for example `libblosc.dylib`).
- `.github/workflows/build.yml`
  - CI runs on Ubuntu with Java 8, installs `libblosc-dev` and `xvfb`, and executes SciJava CI scripts.

## Toolchain and baseline

- Java baseline in CI: **Java 8** (`actions/setup-java` uses zulu 8).
- Build tool: Maven.
- Parent POM: `org.scijava:pom-scijava:42.0.0`.
- Repo uses SciJava/Fiji ecosystem dependencies; avoid ad hoc version bumps unless needed for the task.

## Build and test commands

Run from repository root.

```bash
mvn -DskipTests clean package
```

Notes:
- `pom.xml` defines `<skipTests>true</skipTests>`, so tests are skipped unless explicitly overridden.
- To run tests, override that property:

```bash
mvn -DskipTests=false test
```

- Some tests are GUI/integration-heavy and may require a desktop or virtual display (`xvfb` in CI).

## Change guidelines for agents

- Keep PRs small and focused; do not refactor broad areas unless requested.
- Do not edit generated or build output (`target/`) or local helper artifacts (`mobie-files`, `mobie-table`, `mobie-project`, `mobie-hcs`).
- Preserve public command-line behavior in `org.embl.mobie.cmd.*` unless the task is explicitly about CLI changes.
- When changing I/O or dataset opening behavior, verify at least one representative path (project URI, local files, or table-driven opening).
- Prefer compatibility with existing Java/Fiji runtime assumptions over using newer Java APIs.

## Validation expectations

For code changes, do the minimum validation that matches scope:

1. Compile/package successfully.
2. Run targeted tests related to touched code with `-DskipTests=false` when feasible.
3. For UI/startup changes, smoke-test plugin startup (headless or with ImageJ UI depending on the change).

## Known repository quirks

- `install.sh` contains a hard-coded `VERSION`; if your task involves release/install script maintenance, ensure it matches `pom.xml`.
- CI delegates to remote SciJava scripts (`.github/setup.sh`, `.github/build.sh`), so local behavior can differ from CI details.

## Branch and contribution flow

- Contribution guide expects feature work to branch from `develop` and PRs to target `develop`.
- Keep commit messages and PR descriptions explicit about user-visible behavior changes.

