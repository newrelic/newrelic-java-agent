import com.nr.builder.publish.PublishConfig

plugins {
    `maven-publish`
    signing
    id("com.github.prokod.gradle-crossbuild-scala" )

}
evaluationDependsOn(":newrelic-api")

crossBuild {
    scalaVersionsCatalog = mapOf("2.10" to "2.10.7", "2.11" to "2.11.12", "2.12" to "2.12.13", "2.13" to "2.13.10", "3" to "3.3.1")
    builds {
        register("scala") {
            scalaVersions = setOf("2.10", "2.11", "2.12", "2.13", "3")
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    zinc("org.scala-sbt:zinc_2.13:1.7.1")
    implementation("org.scala-lang:scala-library:2.13.10")
    implementation("org.scala-lang:scala3-library_3:3.3.0")
    implementation(project(":newrelic-api"))
    testImplementation(project(":instrumentation-test"))
    testImplementation(project(path = ":newrelic-agent", configuration = "tests"))
}

val crossBuildScala_210Jar by tasks.getting
val crossBuildScala_211Jar by tasks.getting
val crossBuildScala_212Jar by tasks.getting
val crossBuildScala_213Jar by tasks.getting
val crossBuildScala_3Jar by tasks.getting

val javadocJar by tasks.getting
val sourcesJar by tasks.getting

mapOf(
    "2.10" to crossBuildScala_210Jar,
    "2.11" to crossBuildScala_211Jar,
    "2.12" to crossBuildScala_212Jar,
    "2.13" to crossBuildScala_213Jar,
    "3" to crossBuildScala_3Jar
).forEach { (scalaVersion, versionedClassJar) ->
    PublishConfig.config(
        "crossBuildScala_${scalaVersion.replace(".", "")}",
        project,
        "New Relic Java agent Scala $scalaVersion API",
        "The public Scala $scalaVersion API of the Java agent, and no-op implementations for safe usage without the agent."
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

