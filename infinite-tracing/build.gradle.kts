plugins {
    id("java-library")
}

group = "com.newrelic.agent.java"

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7

    disableAutoTargetJvm()
}

dependencies {
    implementation("com.newrelic.agent.java:infinite-tracing-protobuf:3.1")
    implementation("com.google.guava:guava:28.2-android")
    implementation(project(":agent-model"))
    implementation(project(":agent-interfaces"))
    implementation(project(":newrelic-api"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testImplementation("org.mockito:mockito-junit-jupiter:3.3.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}