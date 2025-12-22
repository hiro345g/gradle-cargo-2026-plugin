package com.bmuschko.gradle.cargo.util

import com.bmuschko.gradle.cargo.util.fixture.HelloWorldServletWarFixture

class DeployableNoContextIntegrationSpec extends AbstractIntegrationSpec {

    HelloWorldServletWarFixture servletWarFixture

    void setup() {
        servletWarFixture = new HelloWorldServletWarFixture(testProjectDir, ":$WAR_CONTEXT")
        configureCargoInstaller()
        buildScript << """
            import com.bmuschko.gradle.cargo.tasks.local.LocalCargoContainerTask

            repositories {
                mavenCentral()
            }

            configurations {
                war
            }

            dependencies {
                war project(path: '${servletWarFixture.projectPath}', configuration: 'archives')
            }
        """
    }

    void cleanup() {
        try {
            runBuild("cargoStopLocal")
        } catch (org.gradle.testkit.runner.UnexpectedBuildFailure e) {
            println "Ignoring expected failure during cargoStopLocal in cleanup: ${e.message}"
        }
    }

    def "can deploy without context parameter"() {
        given:
        buildScript << """
            cargo {
                deployable {
                    file = configurations.war
                }
            }
        """

        when:
        runBuild "cargoStartLocal"

        then:
        requestServletResponseText() == HelloWorldServletWarFixture.RESPONSE_TEXT
    }
}
