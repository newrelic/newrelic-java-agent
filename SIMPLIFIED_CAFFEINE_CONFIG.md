# Simplified Dual Caffeine Configuration

## Matching Your Existing Style

Your existing configurations are simple:

```gradle
configurations {
    tests
    shadowIntoJar
    jarIntoJar
    finalArtifact
}
```

So for dual Caffeine, just add:

```gradle
configurations {
    tests
    shadowIntoJar
    jarIntoJar
    finalArtifact

    // NEW: Simple configurations for dual Caffeine
    caffeine2
    caffeine3
}
```

That's it! The verbose properties (`canBeConsumed`, etc.) are optional and not needed here.

---

## Why I Originally Included Them

I included them as a "best practice" for modern Gradle, but they're **completely optional** for internal configurations like this. They're mainly useful when:

1. Publishing artifacts to Maven repositories
2. Creating complex multi-project builds
3. Defining strict dependency boundaries

For your use case (internal shading), the simple declaration works perfectly.

---

## Updated Gradle Configuration

Here's the **simplified version** of the dual-caffeine setup:

```gradle
// ============================================================================
// STEP 1: Create simple configurations
// ============================================================================

configurations {
    tests
    shadowIntoJar
    jarIntoJar
    finalArtifact

    // NEW: Add these two lines
    caffeine2
    caffeine3
}

configurations.implementation.extendsFrom(configurations.shadowIntoJar)
configurations.implementation.extendsFrom(configurations.jarIntoJar)

// ============================================================================
// STEP 2: Dependencies
// ============================================================================

dependencies {
    // ... existing dependencies ...

    // REMOVE:
    // shadowIntoJar('com.github.ben-manes.caffeine:caffeine:2.9.3')

    // ADD:
    caffeine2('com.github.ben-manes.caffeine:caffeine:2.9.3')
    caffeine3('com.github.ben-manes.caffeine:caffeine:3.2.3')

    // ... rest of dependencies ...
}

// ============================================================================
// STEP 3: Shading tasks (unchanged)
// ============================================================================

task shadeCaffeine2Jar(type: ShadowJar) {
    configurations = [project.configurations.caffeine2]

    relocate('com.github.benmanes.caffeine',
             'com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine')
    relocate('org.checkerframework',
             'com.newrelic.agent.deps.caffeine2.org.checkerframework')

    exclude(
        'META-INF/maven/**',
        'META-INF/*.SF',
        'META-INF/*.DSA',
        'META-INF/*.RSA',
        'module-info.class'
    )

    archiveBaseName.set('caffeine2-shaded')
    archiveClassifier.set('')
    archiveVersion.set('')
    destinationDirectory.set(file("$buildDir/caffeine-shaded"))
}

task shadeCaffeine3Jar(type: ShadowJar) {
    configurations = [project.configurations.caffeine3]

    relocate('com.github.benmanes.caffeine',
             'com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine')
    relocate('org.jspecify',
             'com.newrelic.agent.deps.caffeine3.org.jspecify')

    exclude(
        'META-INF/maven/**',
        'META-INF/*.SF',
        'META-INF/*.DSA',
        'META-INF/*.RSA',
        'module-info.class',
        'META-INF/versions/**'
    )

    archiveBaseName.set('caffeine3-shaded')
    archiveClassifier.set('')
    archiveVersion.set('')
    destinationDirectory.set(file("$buildDir/caffeine-shaded"))
}

// ============================================================================
// STEP 4: Update relocatedShadowJar
// ============================================================================

task relocatedShadowJar(type: ShadowJar) {
    // Add dependencies on Caffeine shading tasks
    dependsOn("classes", "processResources", "generateVersionProperties",
              shadeCaffeine2Jar, shadeCaffeine3Jar)

    from sourceSets.main.output.classesDirs
    from(sourceSets.main.output.resourcesDir) {
        exclude("*.jar")
    }

    // Include both shaded Caffeine JARs
    from(zipTree(shadeCaffeine2Jar.archiveFile.get().asFile))
    from(zipTree(shadeCaffeine3Jar.archiveFile.get().asFile))

    setConfigurations([project.configurations.shadowIntoJar])

    dependencies { filter ->
        filter.exclude(filter.dependency("com.google.code.findbugs:jsr305"))
        filter.exclude(filter.project(":newrelic-api"))
    }

    [
            "com.google", "org.yaml", "org.slf4j", "org.objectweb", "org.json",
            "org.apache.commons", "org.apache.http", "org.apache.logging", "jregex",
            "io.grpc", "com.squareup", "okio", "io.perfmark", "android",
            // NOTE: Removed "com.github.benmanes" since we handle Caffeine separately
            "org.crac",
            "org.apache.log4j", "org.apache.log", "org.apache.avalon",
            "org.checkerframework", "org.dom4j", "org.zeromq", "org.apache.kafka",
            "com.lmax", "com.conversantmedia", "org.jctools",
            "com.fasterxml", "org.osgi", "org.codehaus", "org.fusesource", "kotlin",
            "org.jetbrains", "org.intellij",
            "okhttp3", "org.bouncycastle", "org.conscrypt", "org.openjsse"
    ].each {
        relocate(it, "com.newrelic.agent.deps.$it")
    }

    transform(Log4j2PluginsCacheFileTransformer)

    exclude(
            "**/*.proto",
            "META-INF/maven/**",
            "LICENSE",
            "META-INF/versions/9/module-info.class",
            "META-INF/services/org.apache.logging*",
            "module-info.class",
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/NOTICE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE.txt",
            "META-INF/services/javax.annotation.*"
    )

    mergeServiceFiles()

    archiveBaseName.set("relocatedShadowJar")
}
```

---

## What Changed

**Before (with verbose properties):**
```gradle
configurations {
    caffeine2 {
        canBeConsumed = false
        canBeResolved = true
        transitive = true
    }
    caffeine3 {
        canBeConsumed = false
        canBeResolved = true
        transitive = true
    }
}
```

**After (matching your style):**
```gradle
configurations {
    caffeine2
    caffeine3
}
```

Both work identically! The verbose version just makes the defaults explicit.

---

## Testing the Simplified Version

Update the `dual-caffeine-test/build.gradle` to use the simplified style:

```gradle
configurations {
    caffeine2
    caffeine3
}

dependencies {
    caffeine2('com.github.ben-manes.caffeine:caffeine:2.9.3')
    caffeine3('com.github.ben-manes.caffeine:caffeine:3.2.3')
}

// Rest of the tasks remain the same...
```

Then verify it still works:

```bash
cd dual-caffeine-test
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_202.jdk/Contents/Home gradle clean verifyCaffeineShading
```

---

## Bottom Line

**Use the simple version** - it matches your existing code style and is cleaner. The verbose properties are Gradle "ceremony" that's not needed for internal configurations.