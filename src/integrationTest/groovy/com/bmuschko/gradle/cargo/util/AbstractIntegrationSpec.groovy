package com.bmuschko.gradle.cargo.util

import groovyx.net.http.HttpBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Specification
import spock.lang.TempDir

abstract class AbstractIntegrationSpec extends Specification {

    protected final static String WAR_CONTEXT = 'test-servlet'

    @TempDir
    File testProjectDir

    File buildScript

    void setup() {
        buildScript = new File(testProjectDir, 'build.gradle')
        buildScript << """
            plugins {
                id 'com.bmuschko.cargo'
            }
        """
    }

    void configureCargoInstaller() {
        buildScript << """
            configurations {
                tomcat
            }

            dependencies {
                tomcat "org.apache.tomcat:tomcat:9.0.14@zip"
            }

            cargo {
                containerId = "tomcat9x"
                startStopTimeout = 10000
                
                local {
                    installer {
                        installConfiguration = configurations.tomcat
                        downloadDir = file("\$buildDir/download")
                        extractDir = file("\$buildDir/extract")
                    }
                }
            }
        """
    }

    private void logBuildOutput(String output, String... arguments) {
        def logDir = new File('build/test_log')
        logDir.mkdirs()

        def safeArgs = arguments.join('-').replaceAll('[^a-zA-Z0-9.-]', '_')
        def logFileName = "${getClass().simpleName}-${safeArgs}-${System.currentTimeMillis()}.log"
        def logFile = new File(logDir, logFileName)
        logFile.write(output)

        println "--- Build Output (saved to ${logFile.absolutePath}) ---"
        println output
        println "--- End Build Output ---"
    }

    BuildResult runBuild(String... arguments) {
        def runner = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('-s', *arguments)
                .withPluginClasspath()

        try {
            def result = runner.build()
            logBuildOutput(result.output, *arguments)
            return result
        } catch (UnexpectedBuildFailure e) {
            logBuildOutput(e.buildResult.output, *arguments)
            throw e
        }
    }

    void waitForTomcatStartup() {
        int maxAttempts = 60
        int delaySeconds = 1
        println "Waiting for Tomcat to start... (max ${maxAttempts * delaySeconds} seconds)"

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                new Socket("localhost", 8080).close()
                println "Tomcat is responsive!"
                return
            } catch (java.net.ConnectException e) {
                println "Tomcat not yet responsive (attempt $attempt/$maxAttempts): Failed to connect"
                sleep(delaySeconds * 1000)
            }
        }

        throw new RuntimeException("Tomcat did not start within ${maxAttempts * delaySeconds} seconds.")
    }

    String requestServletResponseText() {
        HttpBuilder.configure {
            request.uri = "http://localhost:8080/$WAR_CONTEXT"
        }.get()
    }
}
