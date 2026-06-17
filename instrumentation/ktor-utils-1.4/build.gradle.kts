plugins {
    id("org.jetbrains.kotlin.jvm")
}

tasks.test {
    systemProperty("newrelic.config.class_transformer.clear_return_stacks", "true")
}


dependencies {
    implementation(project(":agent-bridge"))
    implementation(project(":newrelic-agent"))
    implementation("io.ktor:ktor-utils:1.4.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0")
    testImplementation("io.ktor:ktor-utils:1.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")

}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "com.newrelic.instrumentation.labs.ktor-utils-1.4"
        )
    }
}

verifyInstrumentation {
    passesOnly("io.ktor:ktor-utils:[1.4.0,2.0.0)")
    excludeRegex(".*rc.*")
    excludeRegex(".*beta.*")
}