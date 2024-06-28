import com.nr.builder.publish.PublishConfig

plugins {
    `maven-publish`
    signing
    id("com.github.prokod.gradle-crossbuild-scala" )
}

evaluationDependsOn(":newrelic-api")


crossBuild {
    scalaVersionsCatalog = mapOf("2.13" to "2.13.10")
    builds {
        register("scala") {
            scalaVersions = HashSet<String>().also {
                it.add("2.13")
            }

        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    zinc("org.scala-sbt:zinc_2.13:1.7.1")
    implementation("org.http4s:http4s-ember-server_2.13:0.23.12")
    implementation(project(":newrelic-api"))
    testImplementation(project(":instrumentation-test"))
    testImplementation(project(path = ":newrelic-agent", configuration = "tests"))
}

val crossBuildScala_213Jar by tasks.getting

val javadocJar by tasks.getting
val sourcesJar by tasks.getting

mapOf("2.13" to crossBuildScala_213Jar, ).forEach { (scalaVersion, versionedClassJar) ->
    PublishConfig.config(
            "crossBuildScala_${scalaVersion.replace(".", "")}",
            project,
            "New Relic Java agent instrumentable Scala $scalaVersion http4s ember server processor",
            "The Scala $scalaVersion library for http4s ember server processor. " +
                    "Only intended to be used by instrumentation modules and to be instrumented."
    ) {
        artifact(sourcesJar)
        artifact(javadocJar)
        artifact(versionedClassJar)
    }
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
                "-Dnewrelic.debug=true",
                "-Dnewrelic.sync_startup=true",
                "-Dnewrelic.send_data_on_exit=true",
                "-Dnewrelic.send_data_on_exit_threshold=0",
                "-Dnewrelic.config.log_level=finest",
                "-Dnewrelic.config.startup_log_level=warn",
                "-Dcats.effect.tracing.mode=full",
                "-Dcats.effect.tracing.buffer.size=1024"
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


// This is a functional test really. Jacoco conflicts with the InstrumetnationTestRunner init.
// We will need to resolve this in future work to get Jacoco working with functional tests.
//tasks.withType<Test> {
//    extensions.configure(JacocoTaskExtension::class) {
//        isEnabled = false
//    }
//}