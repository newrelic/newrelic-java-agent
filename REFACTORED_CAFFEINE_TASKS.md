# Refactored Caffeine Shading Tasks

## Original (Duplicated Code)

```gradle
task shadeCaffeine2Jar(type: ShadowJar) {
    group = 'build'
    description = 'Shade Caffeine 2.9.3 into caffeine2 package'
    configurations = [project.configurations.caffeine2]
    relocate('com.github.benmanes.caffeine', 'com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine')
    relocate('org.checkerframework', 'com.newrelic.agent.deps.caffeine2.org.checkerframework')
    exclude('META-INF/maven/**', 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA', 'module-info.class')
    archiveBaseName.set('caffeine2-shaded')
    archiveClassifier.set('')
    archiveVersion.set('')
    destinationDirectory.set(file("$buildDir/caffeine-shaded"))
}

task shadeCaffeine3Jar(type: ShadowJar) {
    group = 'build'
    description = 'Shade Caffeine 3.2.3 into caffeine3 package'
    configurations = [project.configurations.caffeine3]
    relocate('com.github.benmanes.caffeine', 'com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine')
    relocate('org.jspecify', 'com.newrelic.agent.deps.caffeine3.org.jspecify')
    exclude('META-INF/maven/**', 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA', 'module-info.class', 'META-INF/versions/**')
    archiveBaseName.set('caffeine3-shaded')
    archiveClassifier.set('')
    archiveVersion.set('')
    destinationDirectory.set(file("$buildDir/caffeine-shaded"))
}
```

---

## Refactored (DRY - Don't Repeat Yourself)

```gradle
/**
 * Helper method to create a Caffeine shading task.
 *
 * @param taskName Name of the task to create
 * @param version Caffeine version (e.g., "2.9.3" or "3.2.3")
 * @param javaVersions Java versions this is for (e.g., "Java 8-10")
 * @param configurationName Configuration to shade (e.g., "caffeine2")
 * @param packageSuffix Package suffix for relocation (e.g., "caffeine2")
 * @param additionalDeps Additional dependencies to relocate (map of oldPackage -> newPackage)
 * @param additionalExcludes Additional patterns to exclude beyond the defaults
 */
def createCaffeineShadingTask(
        String taskName,
        String version,
        String javaVersions,
        String configurationName,
        String packageSuffix,
        Map<String, String> additionalDeps = [:],
        List<String> additionalExcludes = []) {

    tasks.create(taskName, ShadowJar) {
        group = 'build'
        description = "Shade Caffeine ${version} into ${packageSuffix} package (for ${javaVersions})"

        // Set configuration
        configurations = [project.configurations[configurationName]]

        // Relocate main Caffeine package
        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${packageSuffix}.com.github.benmanes.caffeine")

        // Relocate additional dependencies
        additionalDeps.each { oldPkg, newPkgSuffix ->
            relocate(oldPkg, "com.newrelic.agent.deps.${packageSuffix}.${newPkgSuffix}")
        }

        // Default excludes
        def defaultExcludes = [
                'META-INF/maven/**',
                'META-INF/*.SF',
                'META-INF/*.DSA',
                'META-INF/*.RSA',
                'module-info.class'
        ]

        // Exclude patterns
        exclude(defaultExcludes + additionalExcludes)

        // Archive configuration
        archiveBaseName.set("${packageSuffix}-shaded")
        archiveClassifier.set('')
        archiveVersion.set('')
        destinationDirectory.set(file("$buildDir/caffeine-shaded"))
    }
}

// Create Caffeine 2 shading task
createCaffeineShadingTask(
        'shadeCaffeine2Jar',           // task name
        '2.9.3',                        // version
        'Java 8-10',                    // target Java versions
        'caffeine2',                    // configuration name
        'caffeine2',                    // package suffix
        ['org.checkerframework': 'org.checkerframework']  // additional deps to relocate
)

// Create Caffeine 3 shading task
createCaffeineShadingTask(
        'shadeCaffeine3Jar',           // task name
        '3.2.3',                        // version
        'Java 11+',                     // target Java versions
        'caffeine3',                    // configuration name
        'caffeine3',                    // package suffix
        ['org.jspecify': 'org.jspecify'],  // additional deps to relocate
        ['META-INF/versions/**']        // additional excludes (multi-release JAR)
)
```

---

## Even More Concise Version (Recommended)

If you want it even cleaner, you can use a simpler approach:

```gradle
/**
 * Creates a Caffeine shading task with the given parameters.
 */
def createCaffeineShadingTask(String name, String config, String pkg, Map deps, List excludes = []) {
    tasks.create(name, ShadowJar) {
        group = 'build'
        description = "Shade Caffeine from ${config} configuration"
        configurations = [project.configurations[config]]

        // Relocate Caffeine
        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${pkg}.com.github.benmanes.caffeine")

        // Relocate dependencies
        deps.each { old, suffix ->
            relocate(old, "com.newrelic.agent.deps.${pkg}.${suffix}")
        }

        // Standard excludes
        exclude('META-INF/maven/**', 'META-INF/*.SF', 'META-INF/*.DSA',
                'META-INF/*.RSA', 'module-info.class', *excludes)

        archiveBaseName.set("${pkg}-shaded")
        archiveClassifier.set('')
        archiveVersion.set('')
        destinationDirectory.set(file("$buildDir/caffeine-shaded"))
    }
}

// Create both tasks
createCaffeineShadingTask('shadeCaffeine2Jar', 'caffeine2', 'caffeine2',
        ['org.checkerframework': 'org.checkerframework'])

createCaffeineShadingTask('shadeCaffeine3Jar', 'caffeine3', 'caffeine3',
        ['org.jspecify': 'org.jspecify'],
        ['META-INF/versions/**'])  // Extra exclude for Caffeine 3
```

---

## Benefits

1. ✅ **Less duplication** - Common logic in one place
2. ✅ **Easier to maintain** - Change once, affects both tasks
3. ✅ **Self-documenting** - Parameters make intentions clear
4. ✅ **Extensible** - Easy to add Caffeine 4, 5, etc. in the future
5. ✅ **Type-safe** - Uses proper Gradle task creation API

---

## How It Works

The `createCaffeineShadingTask` method:
1. Creates a new `ShadowJar` task with the given name
2. Configures it with the appropriate configuration
3. Sets up relocation rules for Caffeine and its dependencies
4. Applies excludes (with sensible defaults + custom ones)
5. Configures the output JAR name and location

---

## Usage in Integration Guide

Add this **before** the `relocatedShadowJar` task in `newrelic-agent/build.gradle`:

```gradle
// ============================================================================
// Helper method to create Caffeine shading tasks
// ============================================================================

def createCaffeineShadingTask(String name, String config, String pkg, Map deps, List excludes = []) {
    tasks.create(name, ShadowJar) {
        group = 'build'
        description = "Shade Caffeine from ${config} configuration"
        configurations = [project.configurations[config]]

        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${pkg}.com.github.benmanes.caffeine")

        deps.each { old, suffix ->
            relocate(old, "com.newrelic.agent.deps.${pkg}.${suffix}")
        }

        exclude('META-INF/maven/**', 'META-INF/*.SF', 'META-INF/*.DSA',
                'META-INF/*.RSA', 'module-info.class', *excludes)

        archiveBaseName.set("${pkg}-shaded")
        archiveClassifier.set('')
        archiveVersion.set('')
        destinationDirectory.set(file("$buildDir/caffeine-shaded"))
    }
}

// ============================================================================
// Create Caffeine shading tasks
// ============================================================================

createCaffeineShadingTask('shadeCaffeine2Jar', 'caffeine2', 'caffeine2',
        ['org.checkerframework': 'org.checkerframework'])

createCaffeineShadingTask('shadeCaffeine3Jar', 'caffeine3', 'caffeine3',
        ['org.jspecify': 'org.jspecify'],
        ['META-INF/versions/**'])
```

---

## Alternative: Even More Generic

If you want to be really fancy, you could make it data-driven:

```gradle
// Configuration data
def caffeineConfigs = [
    [
        name: 'shadeCaffeine2Jar',
        config: 'caffeine2',
        pkg: 'caffeine2',
        deps: ['org.checkerframework': 'org.checkerframework'],
        excludes: []
    ],
    [
        name: 'shadeCaffeine3Jar',
        config: 'caffeine3',
        pkg: 'caffeine3',
        deps: ['org.jspecify': 'org.jspecify'],
        excludes: ['META-INF/versions/**']
    ]
]

// Create all tasks from configuration
caffeineConfigs.each { cfg ->
    tasks.create(cfg.name, ShadowJar) {
        group = 'build'
        description = "Shade Caffeine from ${cfg.config} configuration"
        configurations = [project.configurations[cfg.config]]

        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${cfg.pkg}.com.github.benmanes.caffeine")

        cfg.deps.each { old, suffix ->
            relocate(old, "com.newrelic.agent.deps.${cfg.pkg}.${suffix}")
        }

        exclude('META-INF/maven/**', 'META-INF/*.SF', 'META-INF/*.DSA',
                'META-INF/*.RSA', 'module-info.class', *cfg.excludes)

        archiveBaseName.set("${cfg.pkg}-shaded")
        archiveClassifier.set('')
        archiveVersion.set('')
        destinationDirectory.set(file("$buildDir/caffeine-shaded"))
    }
}
```

---

## Recommendation

I recommend the **"Even More Concise Version"** - it's clean, readable, and easy to understand. It strikes a good balance between DRY and clarity.

Would you like me to update the integration guide with this refactored approach?
