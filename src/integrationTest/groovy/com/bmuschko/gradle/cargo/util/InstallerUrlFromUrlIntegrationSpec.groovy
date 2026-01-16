package com.bmuschko.gradle.cargo.util

import com.bmuschko.gradle.cargo.util.fixture.HelloWorldServletWarFixture

class InstallerUrlFromUrlIntegrationSpec extends AbstractIntegrationSpec {

    HelloWorldServletWarFixture servletWarFixture

    void setup() {
        servletWarFixture = new HelloWorldServletWarFixture(testProjectDir, ":$WAR_CONTEXT")
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

            cargo {
                containerId = "tomcat10x"
                
                local {
                    installer {
                        downloadDir = file("\$buildDir/download")
                        extractDir = file("\$buildDir/extract")
                    }
                }
            }
            
            cargo {
                deployable {
                    file = configurations.war
                    context = '$WAR_CONTEXT'
                }
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

    void "url can be used to configure installer source"() {
        given:
        buildScript << """
            cargo {
                local {
                    installer {
                        installUrl = "https://repo1.maven.org/maven2/org/apache/tomcat/tomcat/10.1.50/tomcat-10.1.50.zip"
                    }
                }
            }
        """
        when:
        runBuild "cargoStartLocal"

        then:
        requestServletResponseText() == HelloWorldServletWarFixture.RESPONSE_TEXT
    }
}
