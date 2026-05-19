 plugins {
    id("org.jetbrains.kotlin.jvm")
}

tasks.test {
    systemProperty("newrelic.config.class_transformer.clear_return_stacks", "true")
}


dependencies {
    implementation(project(":agent-bridge"))
    implementation(project(":newrelic-agent"))
    implementation("io.ktor:ktor-server-servlet:1.5.0")
    implementation("javax.servlet:javax.servlet-api:4.0.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0")

}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "com.newrelic.instrumentation.ktor-server-servlet"
        )
    }
}

verifyInstrumentation {
    passesOnly("io.ktor:ktor-server-servlet:[1.0.0,)")
    excludeRegex(".*rc.*")
}