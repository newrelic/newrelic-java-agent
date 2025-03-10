import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.nr.builder.publish.PublishConfig
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.tasks.Jar

plugins {
    `maven-publish`
    signing
    id("com.github.johnrengelman.shadow")
}

tasks.getByName<Jar>("jar") {
    enabled = false
}

val shadowJar = tasks.getByName<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

java {
    withSourcesJar()
    withJavadocJar()
}

val javadocJar by tasks
val sourcesJar by tasks

PublishConfig.config(
        project,
        "New Relic OpenTelemetry Java Agent Extension",
        "Extension for OpenTelemetry Java Agent") {
    artifactId = "newrelic-opentelemetry-agent-extension"
    artifact(shadowJar)
    artifact(javadocJar)
    artifact(sourcesJar)

    // Update artifact version to include "-alpha" suffix
    var artifactVersion = version
    val snapshotSuffix = "-SNAPSHOT"
    artifactVersion = if (artifactVersion.endsWith(snapshotSuffix)) {
        artifactVersion.removeSuffix(snapshotSuffix) + "-alpha" + snapshotSuffix
    } else {
        "$artifactVersion-alpha"
    }
    version = artifactVersion
}

var agent = configurations.create("agent")

var openTelemetryAgentVersion = "2.12.0"
var openTelemetryInstrumentationVersion = "2.12.0-alpha"
var openTelemetrySemConvVersion = "1.22.0-alpha"
var openTelemetryProtoVersion = "1.5.0-alpha"

dependencies {
    implementation(project(":newrelic-api"))

    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${openTelemetryInstrumentationVersion}"))
    compileOnly("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
    compileOnly("com.google.auto.service:auto-service:1.1.1")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:${openTelemetrySemConvVersion}")

    implementation(project(":agent-bridge"))
//    implementation(project(":newrelic-weaver-api"))


    testImplementation("io.opentelemetry:opentelemetry-api")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("io.github.netmikey.logunit:logunit-jul:2.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation("org.assertj:assertj-core:3.24.2")

    agent("io.opentelemetry.javaagent:opentelemetry-javaagent:${openTelemetryAgentVersion}")
}

tasks.withType<Test> {
    dependsOn(shadowJar)
}

testing {
    suites {
        register<JvmTestSuite>("testAgentExtension") {
            dependencies {
                implementation(project(":newrelic-api"))

                implementation("org.mock-server:mockserver-netty:5.15.0:shaded")
                implementation("io.opentelemetry.proto:opentelemetry-proto:${openTelemetryProtoVersion}")
                implementation("org.assertj:assertj-core:3.24.2")
                implementation("org.awaitility:awaitility:4.2.0")
            }

            targets {
                all {
                    testTask {
                        val extensionJar = shadowJar.archiveFile.get().toString()
                        jvmArgs("-javaagent:${agent.singleFile}",
                                "-Dotel.javaagent.extensions=${extensionJar}",
                                "-Dotel.javaagent.logging=none", // Disable logging to avoid connect error logs which occur when the agent has started but the mock server is not yet receiving requests. Set to "simple" to debug.
                                "-Dotel.logs.exporter=otlp",
                                "-Dotel.exporter.otlp.protocol=http/protobuf",
                                "-Dotel.metric.export.interval=500",
                                "-Dotel.bsp.schedule.delay=500",
                                "-Dotel.blrp.schedule.delay=500",
                                "-Dmockserver.disableSystemOut=true" // NOTE: comment out to enable mock server logs for debugging
                        )
                    }
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true

        showStandardStreams = true
    }
}

tasks {
    check {
        dependsOn(testing.suites.getByName("testAgentExtension"))
    }
}
