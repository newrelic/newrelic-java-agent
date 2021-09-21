import com.nr.builder.publish.PublishConfig

plugins {
    `maven-publish`
    `signing`
    scala
}
evaluationDependsOn(":newrelic-api")

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

val javadocJar by tasks.getting
val sourcesJar by tasks.getting

tasks {
    //functional test setup here until scala 2.13 able to be used in functional test project
    test {
        dependsOn("jar")
        setForkEvery(1)
        maxParallelForks = Runtime.getRuntime().availableProcessors()

        val jdk16: String by project
        val jdk15: String by project
        val jdk14: String by project
        val jdk13: String by project
        val jdk12: String by project
        val jdk11: String by project
        val jdk10: String by project
        val jdk9: String by project
        val jdk8: String by project

        if (project.hasProperty("test16")) {
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

