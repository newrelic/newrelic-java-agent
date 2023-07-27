plugins {
    `java-library`
    id("com.github.johnrengelman.shadow")
    id("org.gradle.test-retry") version "1.3.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.test {
    useJUnitPlatform()
    retry {
        maxRetries.set(2)
        maxFailures.set(20)
        failOnPassedAfterRetry.set(true)
    }
}

dependencies {
    implementation(project(":newrelic-weaver"))
    testImplementation(project(":newrelic-weaver-api"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testImplementation("org.mockito:mockito-inline:4.11.0")
}
