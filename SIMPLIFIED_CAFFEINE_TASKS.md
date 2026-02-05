# Simplified Caffeine Shading Tasks

## Ultra-Simplified Version (Recommended)

```gradle
/**
 * Creates a Caffeine shading task for the given major version.
 * @param version Caffeine major version: "2" or "3"
 */
def createCaffeineShadingTask(String version) {
    def config = "caffeine${version}"
    def pkg = "caffeine${version}"

    // Version-specific dependencies to relocate
    def deps = version == "2"
        ? ['org.checkerframework': 'org.checkerframework']
        : ['org.jspecify': 'org.jspecify']

    tasks.create("shadeCaffeine${version}Jar", ShadowJar) {
        group = 'build'
        description = "Shade Caffeine ${version} into ${pkg} package"
        configurations = [project.configurations[config]]

        // Relocate Caffeine
        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${pkg}.com.github.benmanes.caffeine")

        // Relocate version-specific dependencies
        deps.each { old, suffix ->
            relocate(old, "com.newrelic.agent.deps.${pkg}.${suffix}")
        }

        // Standard excludes (same for all versions)
        exclude(
            'META-INF/maven/**',
            'META-INF/*.SF',
            'META-INF/*.DSA',
            'META-INF/*.RSA',
            'META-INF/versions/**',  // Multi-release JAR entries
            'module-info.class'
        )

        archiveBaseName.set("${pkg}-shaded")
        archiveClassifier.set('')
        archiveVersion.set('')
        destinationDirectory.set(file("$buildDir/caffeine-shaded"))
    }
}

// Create both tasks - simple!
createCaffeineShadingTask('2')
createCaffeineShadingTask('3')
```

---

## What Changed

### Before (your suggestion)
```gradle
createCaffeineShadingTask('shadeCaffeine2Jar', 'caffeine2', 'caffeine2',
        ['org.checkerframework': 'org.checkerframework'])

createCaffeineShadingTask('shadeCaffeine3Jar', 'caffeine3', 'caffeine3',
        ['org.jspecify': 'org.jspecify'],
        ['META-INF/versions/**'])
```

### After (ultra-simplified)
```gradle
createCaffeineShadingTask('2')
createCaffeineShadingTask('3')
```

---

## Key Improvements

1. ✅ **Just pass version number** - "2" or "3"
2. ✅ **Convention over configuration** - Derives task name, config name, package name
3. ✅ **Same excludes for both** - `META-INF/versions/**` now in all tasks
4. ✅ **Version-specific deps handled internally** - Uses ternary operator
5. ✅ **Two-line invocation** - Can't get simpler than this!

---

## How It Works

When you call `createCaffeineShadingTask('2')`:
- Creates task: `shadeCaffeine2Jar`
- Uses configuration: `caffeine2`
- Relocates to package: `caffeine2`
- Relocates dependency: `org.checkerframework`

When you call `createCaffeineShadingTask('3')`:
- Creates task: `shadeCaffeine3Jar`
- Uses configuration: `caffeine3`
- Relocates to package: `caffeine3`
- Relocates dependency: `org.jspecify`

---

## Complete Integration Example

Here's how it looks in your `newrelic-agent/build.gradle`:

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

    def deps = version == "2"
        ? ['org.checkerframework': 'org.checkerframework']
        : ['org.jspecify': 'org.jspecify']

    tasks.create("shadeCaffeine${version}Jar", ShadowJar) {
        group = 'build'
        description = "Shade Caffeine ${version} into ${pkg} package"
        configurations = [project.configurations[config]]

        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${pkg}.com.github.benmanes.caffeine")

        deps.each { old, suffix ->
            relocate(old, "com.newrelic.agent.deps.${pkg}.${suffix}")
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

// ============================================================================
// Create Caffeine Shading Tasks
// ============================================================================

createCaffeineShadingTask('2')
createCaffeineShadingTask('3')

// ============================================================================
// Rest of build.gradle...
// ============================================================================

task relocatedShadowJar(type: ShadowJar) {
    dependsOn("classes", "processResources", "generateVersionProperties",
              shadeCaffeine2Jar, shadeCaffeine3Jar)

    // ... rest of configuration
}
```

---

## Benefits

| Aspect | Old Approach | New Approach |
|--------|-------------|--------------|
| **Lines of code** | ~50 lines | ~30 lines |
| **Task invocation** | Complex with 4-5 params | Simple: `'2'` or `'3'` |
| **Adding version 4** | Copy/paste 25 lines | Add one line: `createCaffeineShadingTask('4')` |
| **Maintenance** | Update multiple places | Update one place |
| **Readability** | Medium | High |

---

## Future-Proofing

Adding Caffeine 4 in the future:

```gradle
configurations {
    caffeine2
    caffeine3
    caffeine4  // Just add this
}

dependencies {
    caffeine4('com.github.ben-manes.caffeine:caffeine:4.0.0')  // And this
}

createCaffeineShadingTask('2')
createCaffeineShadingTask('3')
createCaffeineShadingTask('4')  // And this!
```

If Caffeine 4 uses different annotations, update the helper:

```gradle
def deps = version == "2"
    ? ['org.checkerframework': 'org.checkerframework']
    : version == "3"
    ? ['org.jspecify': 'org.jspecify']
    : ['org.newannotations': 'org.newannotations']  // Add case for version 4
```

---

## Testing

Update the POC to use this approach:

```gradle
// In dual-caffeine-test/build.gradle

def createCaffeineShadingTask(String version) {
    def config = "caffeine${version}"
    def pkg = "caffeine${version}"

    def deps = version == "2"
        ? ['org.checkerframework': 'org.checkerframework']
        : ['org.jspecify': 'org.jspecify']

    tasks.create("shadeCaffeine${version}Jar", ShadowJar) {
        group = 'shadow'
        description = "Shade Caffeine ${version} for Java ${version == '2' ? '8-10' : '11+'}"
        configurations = [project.configurations[config]]

        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${pkg}.com.github.benmanes.caffeine")

        deps.each { old, suffix ->
            relocate(old, "com.newrelic.agent.deps.${pkg}.${suffix}")
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
```

---

## Summary

**Two simple lines to create both shading tasks:**

```gradle
createCaffeineShadingTask('2')
createCaffeineShadingTask('3')
```

**That's it!** 🎉
