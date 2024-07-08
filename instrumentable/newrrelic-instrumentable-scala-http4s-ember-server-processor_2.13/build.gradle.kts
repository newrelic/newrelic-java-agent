plugins {
    scala
}

val baseScalaVersion = "2.13"
val scalaLibVersion = "${baseScalaVersion}.14"

configurations.all {
    resolutionStrategy.force("org.scala-lang:scala-library:$scalaLibVersion")
}

tasks.withType<Jar> {
    from("$rootDir/LICENSE")
    manifest {
        attributes["Implementation-Title"] = "New Relic Instrumentable Http4s Ember Server Processor"
        attributes["Implementation-Version"] = project.version
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    zinc("org.scala-sbt:zinc_${baseScalaVersion}:1.7.1")
    implementation("org.http4s:http4s-ember-server_${baseScalaVersion}:0.23.12")
}
