package com.bmuschko.gradle.cargo.util

import com.bmuschko.gradle.cargo.util.fixture.TextResourceFactoryJarFixture
import com.bmuschko.gradle.cargo.util.fixture.TextResourceLoaderServletWarFixture
import okhttp3.OkHttpClient
import okhttp3.Request

class ConfigFileIntegrationSpec extends AbstractIntegrationSpec {
    private final String TEXT_RESOURCE_NAME = "test/resource"
    private final String TEXT_RESOURCE_VALUE = "test resource value"

    void setup() {
        new File(testProjectDir, 'settings.gradle').write """
            pluginManagement {
                repositories {
                    mavenCentral()
                }
            }
        """

        def textResourceFactoryJarFixture = new TextResourceFactoryJarFixture(testProjectDir, "textResourceFactory")
        def textResourceLoaderServletWarFixture = new TextResourceLoaderServletWarFixture(testProjectDir, "textResourceLoader")
        def warBuildScript = new File(textResourceLoaderServletWarFixture.projectDir, 'build.gradle')

        warBuildScript << """
            dependencies {
                implementation project(':textResourceFactory')
            }
        """

        configureCargoInstaller()

        buildScript << """
                                    import groovy.xml.MarkupBuilder
                        
                                    repositories {
                                        mavenCentral()
                                    }
                        
                                    configurations {
                                        war
                                    }
                        
                                    dependencies {
                                        war project(path: 'textResourceLoader', configuration: 'archives')
                                    }
                                    
                                    cargo {
                                        local {
                                            logLevel = 'high'
                                            rmiPort = 8005
                                        }
                                    
                                        deployable {
                                            file = configurations.war
                                            context = '$WAR_CONTEXT'
                                        }
                                    }
            
            task writeContextXml {
                def contextXml = new File(buildDir, "context.xml")
                
                outputs.file(contextXml)
                
                doLast {
                    contextXml.withWriter { writer ->
                        new MarkupBuilder(writer).Context {
                            Resource(
                                name: "$TEXT_RESOURCE_NAME",
                                factory: "TextResourceFactory",
                                value: "$TEXT_RESOURCE_VALUE"
                            )
                        }
                    }
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

    void "can use a file collection as a config files source"() {
        given:
        buildScript << """
            cargo {
                local {
                    configFile {
                        files = writeContextXml.outputs.files
                        toDir = "conf"
                    }
                }
            }
        """

        when:
        runBuild "cargoStartLocal"
        waitForTomcatStartup()

        then:
        requestTextResourceValue(TEXT_RESOURCE_NAME) == TEXT_RESOURCE_VALUE
    }

    String requestTextResourceValue(String resourceName) {
        def client = new OkHttpClient()
        def url = "http://localhost:8080/$WAR_CONTEXT?resourceName=$resourceName"
        def request = new Request.Builder().url(url).build()
        def response = client.newCall(request).execute()
        return response.body().string()
    }
}
