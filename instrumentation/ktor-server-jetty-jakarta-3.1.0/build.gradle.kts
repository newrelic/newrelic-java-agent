plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.test {
    systemProperty("newrelic.config.class_transformer.clear_return_stacks", "true")
}


dependencies {
    implementation(project(":agent-bridge"))
    implementation(project(":newrelic-agent"))
    implementation("io.ktor:ktor-server-jetty-jakarta-jvm:3.1.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0")

}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "com.newrelic.instrumentation.ktor-server-jetty-jakarta-3.1.0"
        )
    }
}

verifyInstrumentation {
    passes("io.ktor:ktor-server-jetty-jakarta-jvm:[3.1.0,3.3.0)")
    excludeRegex("io.ktor:ktor-server-jetty-jakarta-jvm:*-beta*'")
    excludeRegex("io.ktor:ktor-server-jetty-jakarta-jvm:*-rc*")
}