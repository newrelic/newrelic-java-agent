/*
 This gradle script provides the tasks and wiring to add cached weave attributes to instrumentation modules.
 Note that other classes and projects are involved, in buildSrc and in instrumentation-build.
*/

import com.nr.builder.AttributeCommandLineArgumentProvider

// set up a configuration for the classes that write the weaver attributes
val instrumentationBuildConfiguration: Configuration = configurations.create("instrumentationBuild")

// Add an extensible configuration in case the instrumentation will shadow in additional classes
configurations.create("instrumentationWithDependencies")

// the instrumentationBuild configuration consists of the instrumentation-build project
// and its compile dependencies.
dependencies {
    "instrumentationBuild"(project(":instrumentation-build", "shadow"))
}

// Read the compiled instrumentation classes
// Writes a manifest with the weave package attributes
val writeCachedWeaveAttributes = tasks.create<JavaExec>("writeCachedWeaveAttributes") {
    dependsOn("classes")
    dependsOn(":instrumentation-build:shadowJar")

    // We want to recalculate the attributes if any compiled classes have changed.
    // It doesn't matter here if we're using shadow; none of those classes should impact
    // the cached weave attributes.
    inputs.dir("$buildDir/classes")

    val outputFile = "$buildDir/weaveAttributes/MANIFEST.MF"
    outputs.file(outputFile)

    outputs.cacheIf { true }

    classpath = instrumentationBuildConfiguration
    main = "com.nr.instrumentation.builder.CacheWeaveAttributesInManifest"
    argumentProviders.add(AttributeCommandLineArgumentProvider(project, outputFile))
}


// Adds the cached weave attributes to the final jar's manifest.
tasks.named<Jar>("jar") {
    dependsOn(writeCachedWeaveAttributes)
    manifest {
        from(writeCachedWeaveAttributes.outputs.files.first())
    }
}
