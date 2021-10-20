import com.nr.builder.publish.PublishConfig

plugins {
    `maven-publish`
    `signing`
    id("com.github.prokod.gradle-crossbuild-scala")
}
evaluationDependsOn(":newrelic-api")

crossBuild {
    scalaVersionsCatalog = mapOf("2.10" to "2.10.7", "2.11" to "2.11.12", "2.12" to "2.12.13", "2.13" to "2.13.5")
    builds {
        register("scala") {
            scalaVersions = setOf("2.10", "2.11", "2.12", "2.13")
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    zinc("org.scala-sbt:zinc_2.12:1.2.5")
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation(project(":newrelic-api"))
    testImplementation(project(":instrumentation-test"))
    testImplementation(project(path = ":newrelic-agent", configuration = "tests"))
}

val crossBuildScala_210Jar by tasks.getting
val crossBuildScala_211Jar by tasks.getting
val crossBuildScala_212Jar by tasks.getting
val crossBuildScala_213Jar by tasks.getting

val javadocJar by tasks.getting
val sourcesJar by tasks.getting

mapOf(
    "2.10" to crossBuildScala_210Jar,
    "2.11" to crossBuildScala_211Jar,
    "2.12" to crossBuildScala_212Jar,
    "2.13" to crossBuildScala_213Jar
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

        val jdk17: String by project
        val jdk16: String by project
        val jdk15: String by project
        val jdk14: String by project
        val jdk13: String by project
        val jdk12: String by project
        val jdk11: String by project
        val jdk10: String by project
        val jdk9: String by project
        val jdk8: String by project

        if (project.hasProperty("test17")) {
            executable = "$jdk17/bin/java"
            jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        } else if (project.hasProperty("test16")) {
            executable = "$jdk16/bin/java"
            jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        } else if (project.hasProperty("test15")) {
            executable = "$jdk15/bin/java"
        } else if (project.hasProperty("test14")) {
            executable = "$jdk14/bin/java"
        } else if (project.hasProperty("test13")) {
            executable = "$jdk13/bin/java"
        } else if (project.hasProperty("test12")) {
            executable = "$jdk12/bin/java"
        } else if (project.hasProperty("test11")) {
            executable = "$jdk11/bin/java"
        } else if (project.hasProperty("test10")) {
            executable = "$jdk10/bin/java"
            jvmArgs("--add-modules", "java.xml.bind")
        } else if (project.hasProperty("test9")) {
            executable = "$jdk9/bin/java"
            jvmArgs("--add-modules", "java.xml.bind")
        } else if (project.hasProperty("test8")) {
            executable = "$jdk8/bin/java"
        }

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

