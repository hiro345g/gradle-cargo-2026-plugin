#!/bin/sh
list=$(cat << EOS
ConfigFileIntegrationSpec
DefaultDeployableSpec
DeployableFileCollectionIntegrationSpec
DeployableFileIntegrationSpec
DeployableNoContextIntegrationSpec
InstallerUrlFromConfigurationIntegrationSpec
InstallerUrlFromUrlIntegrationSpec
LocallyInstalledContainerIntegrationSpec
EOS
)

for c in ${list}; do
   ./gradlew integrationTest --tests "com.bmuschko.gradle.cargo.util.${c}" --no-daemon --stacktrace --max-workers 1
done
