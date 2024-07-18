import com.nr.builder.publish.PublishConfig

plugins {
    scala
    `maven-publish`
    signing
}
evaluationDependsOn(":newrelic-api")


java {
    withSourcesJar()
    withJavadocJar()
}

//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            artifactId = "newrelic-scala-api_3"
//
//            from(components["java"])
//        }
//    }
//}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.10")
    implementation("org.scala-lang:scala3-library_3:3.3.0")
    implementation(project(":newrelic-api"))
    testImplementation(project(":instrumentation-test"))
    testImplementation(project(path = ":newrelic-agent", configuration = "tests"))
}

val javadocJar by tasks.getting
val sourcesJar by tasks.getting


PublishConfig.config(
        project,
        "New Relic Java agent Scala 3 API",
        "The public Scala 3 API of the Java agent, and no-op implementations for safe usage without the agent."
) {
    from(components["java"])
}

tasks {
    //functional test setup here until scala 2.13 able to be used in functional test project
    test {
        dependsOn("jar")
        setForkEvery(1)
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        minHeapSize = "256m"
        maxHeapSize = "768m"
        val functionalTestArgs = listOf(
                "-javaagent:${com.nr.builder.JarUtil.getNewRelicJar(project(":newrelic-agent")).absolutePath}",
                "-Dnewrelic.config.file=${project(":newrelic-agent").projectDir}/src/test/resources/com/newrelic/agent/config/newrelic.yml",
                "-Dnewrelic.unittest=true",
                "-Dnewrelic.config.startup_log_level=warn"
        )
        jvmArgs(functionalTestArgs + "-Dnewrelic.config.extensions.dir=${projectDir}/src/test/resources/xml_files")
    }

    //no scaladoc jar task, instead this work around makes scaladoc destination folder javadoc to ensure included in jar
    javadoc {
        dependsOn("scaladoc")
    }
    scaladoc {
        val javadocDir = (
                destinationDir.absolutePath
                    .split("/")
                    .dropLast(1)
                    .plus("javadoc")
                ).joinToString("/")
        destinationDir = File(javadocDir)
    }
}

