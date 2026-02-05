# Quick Reference: Gradle Changes for Dual Caffeine

## TL;DR - What to Change in `newrelic-agent/build.gradle`

### 1. Add Configurations (after line 27)

```gradle
configurations {
    tests
    shadowIntoJar
    jarIntoJar
    finalArtifact

    // ADD THESE:
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

### 2. Update Dependencies (around line 100)

```gradle
// REMOVE THIS:
shadowIntoJar('com.github.ben-manes.caffeine:caffeine:2.9.3')

// ADD THESE INSTEAD:
caffeine2('com.github.ben-manes.caffeine:caffeine:2.9.3')
caffeine3('com.github.ben-manes.caffeine:caffeine:3.2.3')
```

### 3. Add Shading Tasks (before relocatedShadowJar, around line 140)

```gradle
// Helper method to create Caffeine shading tasks
def createCaffeineShadingTask(String version) {
    def config = "caffeine${version}"
    def pkg = "caffeine${version}"
    def deps = version == "2" ? ['org.checkerframework'] : ['org.jspecify']

    tasks.create("shadeCaffeine${version}Jar", ShadowJar) {
        group = 'build'
        description = "Shade Caffeine ${version} into ${pkg} package"
        configurations = [project.configurations[config]]

        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${pkg}.com.github.benmanes.caffeine")

        deps.each { dep ->
            relocate(dep, "com.newrelic.agent.deps.${pkg}.${dep}")
        }

        exclude(
            'META-INF/maven/**',
            'META-INF/*.SF',
            'META-INF/*.DSA',
            'META-INF/*.RSA',
            'META-INF/versions/**',
            'module-info.class'
        )

        archiveBaseName.set("${pkg}-shaded")
        archiveClassifier.set('')
        archiveVersion.set('')
        destinationDirectory.set(file("$buildDir/caffeine-shaded"))
    }
}

// Create both shading tasks
createCaffeineShadingTask('2')
createCaffeineShadingTask('3')
```

### 4. Update relocatedShadowJar (around line 157)

```gradle
task relocatedShadowJar(type: ShadowJar) {
    // CHANGE THIS LINE:
    dependsOn("classes", "processResources", "generateVersionProperties")
    // TO THIS:
    dependsOn("classes", "processResources", "generateVersionProperties", shadeCaffeine2Jar, shadeCaffeine3Jar)

    from sourceSets.main.output.classesDirs
    from(sourceSets.main.output.resourcesDir) {
        exclude("*.jar")
    }

    // ADD THESE LINES:
    from(zipTree(shadeCaffeine2Jar.archiveFile.get().asFile))
    from(zipTree(shadeCaffeine3Jar.archiveFile.get().asFile))

    setConfigurations([project.configurations.shadowIntoJar])

    dependencies { filter ->
        filter.exclude(filter.dependency("com.google.code.findbugs:jsr305"))
        filter.exclude(filter.project(":newrelic-api"))
    }

    [
            // stuff we use
            "com.google", "org.yaml", "org.slf4j", "org.objectweb", "org.json",
            "org.apache.commons", "org.apache.http", "org.apache.logging", "jregex",
            "io.grpc", "com.squareup", "okio", "io.perfmark", "android",
            // REMOVE THIS LINE: "com.github.benmanes",
            "org.crac",
            // ... rest
    ].each {
        relocate(it, "com.newrelic.agent.deps.$it")
    }

    // ... rest of task unchanged
}
```

## Verification

After making changes, run:

```bash
# Clean build
./gradlew clean

# Build with dual Caffeine
./gradlew :newrelic-agent:newrelicVersionedAgentJar

# Verify both versions present
jar tf newrelic-agent/build/libs/newrelic-*.jar | grep "Caffeine\.class"

# Should show:
# com/newrelic/agent/deps/caffeine2/.../Caffeine.class
# com/newrelic/agent/deps/caffeine3/.../Caffeine.class
```

## Testing

```bash
# Test on Java 8
./gradlew :newrelic-agent:test -Ptest8

# Test on Java 21
./gradlew :newrelic-agent:test -Ptest21

# Test on Java 11 (when available)
./gradlew :newrelic-agent:test -Ptest26
```

## Files Created

1. **Working POC**: `dual-caffeine-test/` - Standalone proof of concept
2. **Guide**: `DUAL_CAFFEINE_INTEGRATION_GUIDE.md` - Complete implementation guide
3. **Reference**: `dual-caffeine-poc.gradle` - Example Gradle configuration
4. **This file**: Quick reference for the specific changes needed
