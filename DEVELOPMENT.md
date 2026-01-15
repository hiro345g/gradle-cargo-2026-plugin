# Development Guide

This document provides essential instructions for developers and committers of the `gradle-cargo-2026-plugin`. It focuses on building the project, running the test suite, and debugging issues using logs.

## Building and Running the Gradle Project

This plugin is developed using Gradle as its build tool. Operations are primarily conducted via the `gradlew` (Gradle Wrapper) script located in the project root.

### Key Gradle Tasks

Below are the main tasks frequently used in development.

- `./gradlew classes`
  Compiles the source code.

- `./gradlew test`
  Runs the unit tests (`src/test`).

- `./gradlew integrationTest`
  Runs the integration tests (`src/integrationTest`).

- `./gradlew build`
  Performs a full build of the project, including compiling, testing, and generating the JAR file.

- `./gradlew clean`
  Deletes the `build` directory, clearing out previous build artifacts.

- `./gradlew tasks`
  Lists all available tasks.

- `./gradlew dependencies`
  Displays the project's dependency tree, which is useful for investigating library version conflicts.

### Useful Gradle Options

You can control the build behavior by adding the following options when executing tasks.

- `--info`, `--debug`: Increases the verbosity of the build log output. Very useful for debugging problems.
- `--stacktrace`, `--full-stacktrace`: Outputs a more detailed stack trace when an error occurs.
- `--no-daemon`: Runs the build without using the Gradle daemon (a background process). Useful for ensuring a completely isolated environment.
- `-x <task>`: Excludes the specified task from the build (e.g., `./gradlew build -x test` builds the project without running tests).

## Overview of Build Scripts

The build logic for this project is primarily managed in two files.

- `build.gradle`
  Located at the project root, this file defines the basic settings common to the entire plugin. Key responsibilities include:

  - Defining project information such as `group` and `version`.
  - Applying the `java` and `groovy` plugins.
  - Defining project-wide dependencies (e.g., Groovy, Spock).
  - Applying the `gradle/integration-test.gradle` script.

- `gradle/integration-test.gradle`
  This script is dedicated to settings related to integration tests (`src/integrationTest`). It is applied from `build.gradle` to modularize the build configuration. Key responsibilities include:
  - Defining `src/integrationTest` as a source set.
  - Defining dependencies used only for integration tests (e.g., Gradle TestKit, Cargo libraries).
  - Customizing the behavior of the `integrationTest` task (e.g., setting system properties).

## Running Tests

The test suite, particularly the integration tests, is critical for ensuring plugin stability. Due to the nature of Gradle TestKit and container lifecycle management, running tests requires specific procedures.

### Running a Single Integration Test

To debug a specific issue, it's often most efficient to run a single test class. Use the following command format. This example assumes an environment where Java `17.0.17-tem` is set up using `SDKMAN!`.

```bash
JAVA_HOME=${HOME}/.sdkman/candidates/java/17.0.17-tem \
  ./gradlew clean integrationTest --tests "com.bmuschko.gradle.cargo.util.ConfigFileIntegrationSpec" --no-daemon --stacktrace
```

- `--tests "..."`: This filter is essential for isolating a specific test.
- `--no-daemon`: Use this to ensure a clean, isolated environment for the test run, which helps in achieving reproducible results.
- `--stacktrace`: Provides detailed stack traces on failure.
- `clean` task: Be cautious about using `clean`, as it will delete the log directories from previous runs. If you need to inspect logs, run the test without `clean`.

### Debugging with Logs

Logs are crucial for debugging, as standard output may not capture all relevant information. The logs are generated in `.gitignore`'d directories.

#### Test-Generated Logs

The `AbstractIntegrationSpec` class writes the build outcome of each test execution to a log file. This is the first place to look for test failures.

- **Location:** `build/test_log/`
- **Example File Name:** `ConfigFileIntegrationSpec-cargoStartLocal-*.log`
- **How to View:** Find the most recent log file and view its content using shell commands.

  ```bash
  # Find the latest log
  ls -t build/test_log/

  # View the content
  cat build/test_log/<log-file-name>.log
  ```

#### Gradle TestKit Daemon Logs

The Gradle TestKit uses a dedicated daemon process for executing tests. Its logs contain low-level details about the Gradle build process that are invaluable for debugging deep-seated issues.

- **Location:** `build/tmp/integrationTest/work/.gradle-test-kit/test-kit-daemon/{gradle-version}/`
- **Example File Name:** `daemon-*.out.log`
- **How to View:**

  ```bash
  # Find the latest daemon log for a specific Gradle version
  ls -t build/tmp/integrationTest/work/.gradle-test-kit/test-kit-daemon/7.6.4/*.log

  # View the content
  cat build/tmp/integrationTest/work/.gradle-test-kit/test-kit-daemon/7.6.4/<daemon-log-name>.log
  ```

### Running the Full Integration Test Suite

During the upgrade from Gradle 6 to 9, running the full integration test suite with `./gradlew integrationTest` was time-consuming. Additionally, some tests that passed individually would fail when run as part of the full suite, requiring repeated test runs.

For this reason, the `src/test/test.sh` script was created to run individual tests in sequence. This script can be used for troubleshooting when the full `./gradlew integrationTest` suite does not pass.

**How to Run:**

```bash
JAVA_HOME=${HOME}/.sdkman/candidates/java/17.0.17-tem bash src/test/test.sh
```

## Verifying Changes with a Local Project

Before committing changes, you may want to verify them in a separate, local test project. The recommended way to do this is with a **Composite Build**.

1. In your local test project's `settings.gradle`, add an `includeBuild` directive pointing to your clone of `gradle-cargo-2026-plugin`.
2. In the `build.gradle` of the test project, apply the plugin by its ID. No version is required.

`local-test-project/settings.gradle`:

```groovy
includeBuild '../path/to/your/gradle-cargo-2026-plugin'
```

`local-test-project/build.gradle`:

```groovy
plugins {
    id 'io.github.hiro345g.cargo-2026'
}
```

This setup ensures that any code change in the plugin is immediately reflected when you build the test project.
