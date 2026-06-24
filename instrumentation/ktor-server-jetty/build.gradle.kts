plugins {
    id("org.jetbrains.kotlin.jvm")
}

tasks.test {
    systemProperty("newrelic.config.class_transformer.clear_return_stacks", "true")
}


dependencies {
    implementation(project(":agent-bridge"))
    implementation(project(":newrelic-agent"))
    implementation("io.ktor:ktor-server-jetty-jvm:2.0.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0")

}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "com.newrelic.instrumentation.ktor-server-jetty"
        )
    }
}

verifyInstrumentation {
    passesOnly("io.ktor:ktor-server-jetty-jvm:[2.0.0,)")
    excludeRegex(".*beta.*")
    excludeRegex(".*rc.*")
}