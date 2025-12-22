package com.bmuschko.gradle.cargo.util

import com.bmuschko.gradle.cargo.util.fixture.HelloWorldServletWarFixture

class DeployableFileIntegrationSpec extends AbstractIntegrationSpec {

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

    def "can use a file as a deployable"() {
        given:
        buildScript << """
            task configureCargoDeployable {
                inputs.files(configurations.war)
                
                doLast {
                    cargo {
                        deployable {
                            file = configurations.war.singleFile
                            context = '$WAR_CONTEXT'
                        }
                    }
                }
            }
            
            tasks.withType(LocalCargoContainerTask) {
                dependsOn configureCargoDeployable
            }
        """

        when:
        runBuild "cargoStartLocal"

        then:
        requestServletResponseText() == HelloWorldServletWarFixture.RESPONSE_TEXT
    }
}
