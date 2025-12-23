package com.bmuschko.gradle.cargo.util

import okhttp3.OkHttpClient
import okhttp3.Request
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
        def serverXml = new File(testProjectDir, 'server.xml')
        serverXml.text = """<?xml version='1.0' encoding='utf-8'?>
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />

  <Service name="Catalina">
    <Connector port="${System.properties.getProperty('cargo.servlet.port', '8080')}" protocol="HTTP/1.1" connectionTimeout="20000" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="true">
      </Host>
    </Engine>
  </Service>
</Server>
"""
    }

    void configureCargoInstaller() {
        buildScript << """
            configurations {
                tomcat
            }

            dependencies {
                tomcat "org.apache.tomcat:tomcat:10.1.50@zip"
            }

            cargo {
                containerId = "tomcat10x"
                startStopTimeout = 10000
                
                local {
                    installer {
                        installConfiguration = configurations.tomcat
                        downloadDir = file("\$buildDir/download")
                        extractDir = file("\$buildDir/extract")
                    }
                    configFile {
                        files = project.files('server.xml')
                        toDir = 'conf'
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
        def client = new OkHttpClient()
        def request = new Request.Builder().url("http://localhost:8080/$WAR_CONTEXT").build()
        client.newCall(request).execute().withCloseable { response ->
            return response.body().string()
        }
    }
}
