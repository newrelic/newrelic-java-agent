plugins { scala }
scala.zincVersion.set("1.2.5")

evaluationDependsOn(":newrelic-agent")

tasks.jar {
    manifest {
        attributes("Implementation-Title" to "com.newrelic.weaved-scala-reflection-test",
        "Implementation-Title-Alias" to "weaved-scala-reflection-test")
    }
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.5")
    implementation("com.typesafe.akka:akka-http_2.13:10.1.8")
    implementation("com.typesafe.akka:akka-stream_2.13:2.5.23")

    testImplementation("org.scala-lang:scala-reflect:2.13.5")
    testImplementation(project(":instrumentation-test"))
    testImplementation(project(path = ":newrelic-agent", configuration = "tests"))
}


tasks {
    //functional test setup here until scala 2.13 able to be used in functional test project
    test {
        onlyIf {
            !project.hasProperty("test7")
        }

        dependsOn("jar")
        setForkEvery(1)
        maxParallelForks = Runtime.getRuntime().availableProcessors()

        val jdk15: String by project
        val jdk14: String by project
        val jdk13: String by project
        val jdk12: String by project
        val jdk11: String by project
        val jdk10: String by project
        val jdk9: String by project
        val jdk8: String by project

        if (project.hasProperty("test15")) {
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
            "-Dnewrelic.debug=true",
            "-Dnewrelic.sync_startup=true",
            "-Dnewrelic.send_data_on_exit=true",
            "-Dnewrelic.send_data_on_exit_threshold=0",
            "-Dnewrelic.config.log_level=finest",
            "-Dnewrelic.config.startup_log_level=warn"
        )
        jvmArgs(functionalTestArgs + "-Dnewrelic.config.extensions.dir=${projectDir}/src/test/resources/xml_files")
    }
}

