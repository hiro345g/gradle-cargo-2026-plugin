# Usage Guide for gradle-cargo-2026-plugin

Note: This plugin has not yet been published to public Maven repositories (e.g., Maven Central). Please install it to your local Maven repository before use.

## 1. Clone and Install the Plugin Locally

Open your terminal and follow the steps below.

This fork supports Gradle 9. We will clone the repository into the `build-cargo-plugin/gradle-cargo-2026-plugin` directory, switch to the `gradle-cargo-2026` branch, and publish it to your local Maven repository.

```bash
# Clone the plugin repository
git clone https://github.com/hiro345g/gradle-cargo-2026-plugin.git build-cargo-plugin/gradle-cargo-2026-plugin

# Switch to the specific branch
cd build-cargo-plugin/gradle-cargo-2026-plugin && git switch gradle-cargo-2026

# Ensure you are using Java 17. You can check or set it using the `sdk` command.
sdk env

# Publish to your local Maven repository (~/.m2/repository)
./gradlew publishToMavenLocal

```

## 2. Configuration Examples (build.gradle, settings.gradle, libs.versions.toml)

The following examples demonstrate a project for a WAR application (`webapp001`) under the group `internal.dev.app001`. This setup assumes the use of Gradle Wrapper and Version Catalogs (`libs.versions.toml`).

```text
webapp001/
├── build.gradle
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── gradlew
├── gradlew.bat
├── settings.gradle
└── src/
```

`build.gradle`

```gradle
plugins {
    alias(libs.plugins.cargo)
    id 'war'
    id 'eclipse-wtp'
}

repositories {
    mavenLocal() // Required to find the locally published plugin
    mavenCentral()
}

configurations {
    tomcat
}

dependencies {
    // Servlet API & Testing
    providedCompile libs.jakarta.servlet.api
    testImplementation libs.junit.junit

    // Define Tomcat distribution for the container
    tomcat libs.tomcat.get().toString() + '@zip'
}

group = 'internal.dev.app001'
version = '1.0-SNAPSHOT'
description = 'webapp001'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
}

tasks.withType(Javadoc) {
    options.encoding = 'UTF-8'
}

war {
    webAppDirectory = file('src/main/webapp')
}

// Task to extract Tomcat for local execution
task installTomcat(type: Copy) {
    from { zipTree(configurations.tomcat.singleFile) }
    into "$buildDir/tomcat-home"
    eachFile { FileCopyDetails fileCopyDetails ->
        def original = fileCopyDetails.relativePath
        // Strip the top-level directory from the zip
        fileCopyDetails.relativePath = new RelativePath(original.file, *original.segments[1..-1])
    }
}

cargo {
    containerId = 'tomcat10x'
    port = 8080

    deployable {
        context = 'ROOT'
    }

    local {
        homeDir = installTomcat.outputs.files.singleFile
    }
}

afterEvaluate {
    cargoStartLocal.dependsOn installTomcat
}

// Custom Task Registration
tasks.register('start', com.bmuschko.gradle.cargo.tasks.local.CargoStartLocal) {
    dependsOn installTomcat
}

tasks.register('stop', com.bmuschko.gradle.cargo.tasks.local.CargoStopLocal)

tasks.register('start-debug', com.bmuschko.gradle.cargo.tasks.local.CargoStartLocal) {
    dependsOn installTomcat
    jvmArgs = '-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005'
    rmiPort = 8206
}

tasks.register('stop-debug', com.bmuschko.gradle.cargo.tasks.local.CargoStopLocal) {
    rmiPort = 8206
}
```

`settings.gradle`

```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal() // Look for the plugin in the local repository
    }
}

rootProject.name = 'webapp001'
```

`gradle/libs.versions.toml`

```toml
[versions]
junit-junit = "4.13.1"
cargo = "1.0.0"
jakarta-servlet-api = "6.0.0"
tomcat = "10.1.20"

[libraries]
junit-junit = { module = "junit:junit", version.ref = "junit-junit" }
jakarta-servlet-api = { module = "jakarta.servlet:jakarta.servlet-api", version.ref = "jakarta-servlet-api" }
tomcat = { module = "org.apache.tomcat:tomcat", version.ref = "tomcat" }

[plugins]
cargo = { id = "io.github.hiro345g.cargo-2026", version.ref = "cargo" }

```
