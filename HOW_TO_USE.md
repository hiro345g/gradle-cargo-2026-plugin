# How to Use the Unofficial `gradle-cargo-plugin`

This document provides a comprehensive guide on how to integrate the unofficial `gradle-cargo-plugin` from [https://github.com/hiro345g/gradle-cargo-plugin](https://github.com/hiro345g/gradle-cargo-plugin) into your Gradle project.

Since this is not an official release published to the Gradle Plugin Portal, you need to use one of the following methods. Please choose the one that best fits your project's requirements for build reproducibility and security policies.

## Choosing the Right Method

| Method                          | Recommended Scenario                                                                                                 | Pros                                                                | Cons                                                                                   |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| **1. Private Maven Repository** | **Organizational use.** For teams that need stable, reproducible builds and cannot rely on external services.        | High reproducibility and security. Central management of artifacts. | Requires setup of a private repository (e.g., Artifactory, Nexus).                     |
| **2. Local JAR File**           | **Simple or offline projects.** When you want to use a fixed version of the plugin without complex repository setup. | High reproducibility once set up. Works offline.                    | Manual process to distribute the JAR file across a team.                               |
| **3. JitPack**                  | **Quick tests or individual use.** When you need an easy way to test a specific branch or commit.                    | Very easy to set up for a single developer.                         | May not be allowed by organizational security policies. Relies on an external service. |
| **4. Composite Builds**         | **Plugin development.** For developers who are actively contributing to or debugging the plugin itself.              | Instant feedback on code changes; best IDE integration.             | Not suitable for general use; requires a local clone of the plugin repository.         |

---

## Method 1: Using a Private Maven Repository (Recommended for Organizational Use)

This is the most robust and secure method for teams, ensuring that builds are reproducible and do not depend on external services.

### 1.1. For the person publishing the plugin:

You first need to publish the plugin artifact to your organization's private Maven repository (e.g., Artifactory, Nexus).

1.  **Clone the `gradle-cargo-plugin` repository** and check out the desired version (a specific branch, tag, or commit).
2.  **Configure publishing settings.** Add the `maven-publish` plugin to `build.gradle` and configure it with your repository's URL and credentials.
    ```groovy
    // build.gradle
    publishing {
        repositories {
            maven {
                url = "https://your.company.repo/maven-releases"
                credentials {
                    username = "your-username"
                    password = "your-password"
                }
            }
        }
    }
    ```
3.  **Publish the artifact** by running the following command in the plugin's project directory:
    ```bash
    ./gradlew publish
    ```

### 1.2. For the plugin consumer:

1.  **Edit `settings.gradle`** to add your private Maven repository.
    ```groovy
    // settings.gradle
    pluginManagement {
        repositories {
            maven { url "https://your.company.repo/maven-releases" }
            gradlePluginPortal()
            mavenCentral()
        }
    }
    ```
2.  **Edit `build.gradle`** to apply the plugin with the version you published.
    ```groovy
    // build.gradle
    plugins {
        id 'com.bmuschko.cargo' version '2.12.0' // Use the version you published
    }
    ```

---

## Method 2: Using a Local Plugin JAR

This method is suitable for simple projects or for teams where distributing a JAR file manually is feasible. It ensures high build reproducibility as the artifact is stored locally.

1.  **Prepare the JAR File**: Generate the JAR from the plugin project with `./gradlew assemble`.
2.  **Place the JAR**: Create a directory in your project (e.g., `local_plugins`) and copy the JAR file into it.
3.  **Configure `settings.gradle`**: Add a `flatDir` repository pointing to your local directory.
    ```groovy
    // settings.gradle
    pluginManagement {
        repositories {
            flatDir { dirs 'local_plugins' }
            gradlePluginPortal()
            mavenCentral()
        }
    }
    ```
4.  **Apply the Plugin in `build.gradle`**:
    ```groovy
    // build.gradle
    plugins {
        id 'com.bmuschko.cargo' version '{plugin_version}' // Use the exact version of the JAR
    }
    ```

---

## Method 3: Using JitPack (For Quick Tests and Individual Use)

This method is convenient but may not be suitable for all environments. **If your organization has strict security policies regarding external build services, please use Method 1 or 2.**

For reproducible builds, it is highly recommended to use a **specific commit hash** as the version instead of a branch name (`-SNAPSHOT`).

1.  **Edit `settings.gradle`**: Add the JitPack repository.
    ```groovy
    // settings.gradle
    pluginManagement {
        repositories {
            maven { url 'https://jitpack.io' }
            gradlePluginPortal()
            mavenCentral()
        }
    }
    ```
2.  **Edit `build.gradle`**: Apply the plugin, preferably using a commit hash for the version.

    ```groovy
    // build.gradle
    plugins {
        // Recommended: Use a specific commit hash for reproducible builds
        id 'com.github.hiro345g.gradle-cargo-plugin.com.bmuschko.cargo' version 'a1b2c3d4e5'

        // For temporary testing, you can use a branch name
        // id 'com.github.hiro345g.gradle-cargo-plugin.com.bmuschko.cargo' version 'gradle9-SNAPSHOT'
    }
    ```

---

## Method 4: Using Composite Builds (For Plugin Development)

This method is intended only for those who are actively developing or debugging the plugin itself. It links the plugin's source code directly to your build.

1.  **Clone the plugin repository** locally.
2.  **Edit `settings.gradle`**: In your test project, use `includeBuild` to point to your local clone.
    ```groovy
    // settings.gradle
    includeBuild '../path/to/your/gradle-cargo-plugin'
    ```
3.  **Edit `build.gradle`**: Apply the plugin by ID, without a version.
    ```groovy
    // build.gradle
    plugins {
        id 'com.bmuschko.cargo'
    }
    ```
