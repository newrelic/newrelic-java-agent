plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    onlyIf { !project.hasProperty("test8") && !project.hasProperty("test11") }
    systemProperty("newrelic.config.class_transformer.clear_return_stacks", "true")
}


dependencies {
    implementation(project(":agent-bridge"))
    implementation(project(":newrelic-agent"))
    implementation("io.ktor:ktor-server-jetty-jakarta-jvm:3.3.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0")

}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "com.newrelic.instrumentation.ktor-server-jetty-jakarta-3.3.0"
        )
    }
}

verifyInstrumentation {
    passesOnly("io.ktor:ktor-server-jetty-jakarta-jvm:[3.3.0,)")
    excludeRegex(".*beta.*")
    excludeRegex(".*rc.*")
}