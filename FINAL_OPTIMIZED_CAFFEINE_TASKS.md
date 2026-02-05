# Final Optimized Caffeine Shading Tasks

## Before (Map with Duplicate Key/Value)

```gradle
def deps = version == "2"
    ? ['org.checkerframework': 'org.checkerframework']
    : ['org.jspecify': 'org.jspecify']

deps.each { old, suffix ->
    relocate(old, "com.newrelic.agent.deps.${pkg}.${suffix}")
}
```

---

## After (Simple List)

```gradle
def deps = version == "2" ? ['org.checkerframework'] : ['org.jspecify']

deps.each { pkg ->
    relocate(pkg, "com.newrelic.agent.deps.caffeine${version}.${pkg}")
}
```

---

## Complete Optimized Method

```gradle
/**
 * Creates a Caffeine shading task for the given major version.
 * @param version Caffeine major version: "2" or "3"
 */
def createCaffeineShadingTask(String version) {
    def config = "caffeine${version}"
    def pkg = "caffeine${version}"
    def deps = version == "2" ? ['org.checkerframework'] : ['org.jspecify']

    tasks.create("shadeCaffeine${version}Jar", ShadowJar) {
        group = 'build'
        description = "Shade Caffeine ${version} into ${pkg} package"
        configurations = [project.configurations[config]]

        // Relocate Caffeine
        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${pkg}.com.github.benmanes.caffeine")

        // Relocate version-specific dependencies
        deps.each { dep ->
            relocate(dep, "com.newrelic.agent.deps.${pkg}.${dep}")
        }

        // Standard excludes
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

---

## Benefits

1. ✅ **Simpler data structure** - List instead of Map
2. ✅ **More obvious intent** - Clear that we're just relocating these packages
3. ✅ **Less verbose** - No duplicate key/value pairs
4. ✅ **Easier to read** - Single iteration variable instead of two

---

## Side-by-Side Comparison

| Aspect | Map Version | List Version |
|--------|-------------|--------------|
| **Declaration** | `['org.checkerframework': 'org.checkerframework']` | `['org.checkerframework']` |
| **Iteration** | `deps.each { old, suffix -> ... }` | `deps.each { dep -> ... }` |
| **Relocate call** | `relocate(old, "...${suffix}")` | `relocate(dep, "...${dep}")` |
| **Clarity** | Medium (why duplicate?) | High (obvious intent) |
| **Bytes** | 63 chars | 25 chars |

---

## Full Integration Example

```gradle
configurations {
    tests
    shadowIntoJar
    jarIntoJar
    finalArtifact
    caffeine2
    caffeine3
}

dependencies {
    // ... existing dependencies ...

    caffeine2('com.github.ben-manes.caffeine:caffeine:2.9.3')
    caffeine3('com.github.ben-manes.caffeine:caffeine:3.2.3')

    // ... rest of dependencies ...
}

// ============================================================================
// Caffeine Shading Helper
// ============================================================================

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

createCaffeineShadingTask('2')
createCaffeineShadingTask('3')

// ============================================================================
// Update relocatedShadowJar
// ============================================================================

task relocatedShadowJar(type: ShadowJar) {
    dependsOn("classes", "processResources", "generateVersionProperties",
              shadeCaffeine2Jar, shadeCaffeine3Jar)

    from sourceSets.main.output.classesDirs
    from(sourceSets.main.output.resourcesDir) {
        exclude("*.jar")
    }

    // Include both shaded Caffeine JARs
    from(zipTree(shadeCaffeine2Jar.archiveFile.get().asFile))
    from(zipTree(shadeCaffeine3Jar.archiveFile.get().asFile))

    // ... rest of configuration
}
```

---

## Multiple Dependencies (If Needed)

If a future Caffeine version has multiple dependencies to relocate:

```gradle
def deps = version == "2"
    ? ['org.checkerframework']
    : version == "3"
    ? ['org.jspecify']
    : ['org.jspecify', 'org.someother', 'org.another']  // Version 4 example

deps.each { dep ->
    relocate(dep, "com.newrelic.agent.deps.${pkg}.${dep}")
}
```

---

## Evolution

**Version 1 (Original)**: 50 lines, duplicated code
**Version 2 (Helper with map)**: 30 lines, map with duplicate key/value
**Version 3 (Helper with list)**: 28 lines, clean list ✅

---

Perfect! This is now as clean as it can get. 🎉
