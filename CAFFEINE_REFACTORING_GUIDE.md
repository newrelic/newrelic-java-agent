# Caffeine Refactoring Guide - Complete Implementation Plan

## Overview

This guide shows how to refactor all direct Caffeine usage to use `AgentCollectionFactory` instead. This enables runtime selection between Caffeine 2.9.3 (Java 8-10) and Caffeine 3.2.3 (Java 11+).

---

## Phase 0: Build Configuration for Shaded Packages

### Problem 1: Compile-Time vs Runtime Package Names

The factory classes need to import from the shaded package names (e.g., `com.newrelic.agent.deps.caffeine2.*`), but these packages don't exist until the shading tasks run. We need to:

1. Build the shaded JARs FIRST
2. Add them to the compile classpath
3. Ensure compilation happens AFTER shading

### Problem 2: Cannot Compile Caffeine3 Classes with JDK 8

When compiling with JDK 8, the compiler cannot read Caffeine 3.2.3 class files (which are Java 11+). The solution is a **Multi-Release JAR**:

```
newrelic.jar:
├── com/newrelic/agent/util/
│   ├── AgentCollectionFactory.class           (Java 8, delegates to Caffeine2)
│   └── Caffeine2CollectionFactory.class       (Java 8)
└── META-INF/
    └── versions/
        └── 11/
            └── com/newrelic/agent/util/
                ├── AgentCollectionFactory.class       (Java 11, delegates to Caffeine3)
                └── Caffeine3CollectionFactory.class   (Java 11)
```

- Java 8-10 runtime: Uses base classes (Caffeine 2)
- Java 11+ runtime: Uses classes from META-INF/versions/11/ (Caffeine 3)

### Important: Task Definition Order in build.gradle

⚠️ **Critical:** In Gradle, tasks must be defined BEFORE they're referenced. The correct order in `newrelic-agent/build.gradle` is:

1. **First:** Configure source sets for Multi-Release JAR (Java 8 + Java 11)
2. **Then:** Define `createCaffeineShadingTask()` helper method and create the tasks
3. **Then:** Reference the tasks in the `dependencies` block
4. **Finally:** Add task dependencies to `compileJava` and `compileJava11Java`

### Step 0: Configure Source Sets for Multi-Release JAR

Add this near the top of `build.gradle`, after the `java` configuration block:

```gradle
// Configure source sets for Multi-Release JAR
sourceSets {
    java11 {
        java {
            srcDirs = ['src/main/java11']
        }
    }
}

// Make java11 source set inherit dependencies from main
// (Add this right after your existing configurations block)
configurations {
    java11Implementation.extendsFrom(implementation)
    java11CompileOnly.extendsFrom(compileOnly)
}

// Configure Java 11 compilation with Java 11 toolchain
tasks.named('compileJava11Java') {
    // Use Java 11 toolchain for compiling Java 11 sources
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(11)
    }
    // Set source and target compatibility
    sourceCompatibility = '11'
    targetCompatibility = '11'
}
```

**What this does:**
- Creates a `java11` source set for Java 11-specific code
- Java 11 code goes in `src/main/java11/` directory
- **Makes java11 inherit all dependencies from main** (including agent-bridge where CollectionFactory lives)
- Configures the compiler to use Java 11 toolchain (even when building with Java 8)
- Sets source/target compatibility to Java 11

### Step 1: Define Shading Tasks (Must Come First)

Add these BEFORE your `dependencies` block:

```gradle
// Helper method to create shading tasks
def createCaffeineShadingTask(String version) {
    def config = "caffeine${version}"
    def pkg = "caffeine${version}"
    def deps = version == "2" ? ['org.checkerframework'] : ['org.jspecify']

    tasks.create("shadeCaffeine${version}Jar", ShadowJar) {
        configurations = [project.configurations[config]]
        relocate('com.github.benmanes.caffeine',
                "com.newrelic.agent.deps.${pkg}.com.github.benmanes.caffeine")
        deps.each { dep ->
            relocate(dep, "com.newrelic.agent.deps.${pkg}.${dep}")
        }
        exclude('META-INF/maven/**', 'META-INF/*.SF', 'META-INF/*.DSA',
                'META-INF/*.RSA', 'META-INF/versions/**', 'module-info.class')
        archiveBaseName.set("${pkg}-shaded")
        archiveClassifier.set('')
        archiveVersion.set('')
        destinationDirectory.set(file("$buildDir/caffeine-shaded"))
    }
}

// Create the two shading tasks
createCaffeineShadingTask('2')
createCaffeineShadingTask('3')
```

### Step 2: Update dependencies Block (References Tasks)

Now that tasks are defined, add these to your `dependencies` block:

```gradle
dependencies {
    // ... existing dependencies ...

    // For IDE support and base API understanding
    compileOnly('com.github.ben-manes.caffeine:caffeine:2.9.3')

    // These will be shaded into separate packages
    caffeine2('com.github.ben-manes.caffeine:caffeine:2.9.3')
    caffeine3('com.github.ben-manes.caffeine:caffeine:3.2.3')

    // Make shaded JARs available at compile time for main source set (Java 8)
    compileOnly files(shadeCaffeine2Jar.outputs.files)

    // Make both shaded JARs available for java11 source set
    java11CompileOnly files(shadeCaffeine2Jar.outputs.files)
    java11CompileOnly files(shadeCaffeine3Jar.outputs.files)

    // Java 11 source set needs access to main classes
    java11Implementation sourceSets.main.output
}
```

### Step 3: Update Compilation Tasks

Add this AFTER the `dependencies` block:

```gradle
// Ensure shaded JARs are built BEFORE compilation

// Main source set (Java 8) - only needs Caffeine 2
compileJava {
    dependsOn shadeCaffeine2Jar
    // Exclude Caffeine3CollectionFactory from main compilation
    exclude '**/Caffeine3CollectionFactory.java'
}

// Java 11 source set - needs both Caffeine 2 and 3
compileJava11Java {
    dependsOn shadeCaffeine2Jar, shadeCaffeine3Jar
}
```

### Step 4: Organize Source Files

Create the directory structure and move files:

```bash
# Create java11 source directory
mkdir -p newrelic-agent/src/main/java11/com/newrelic/agent/util

# Copy Caffeine3CollectionFactory to java11 source set
cp newrelic-agent/src/main/java/com/newrelic/agent/util/Caffeine3CollectionFactory.java \
   newrelic-agent/src/main/java11/com/newrelic/agent/util/

# Copy AgentCollectionFactory to java11 source set (will be modified)
cp newrelic-agent/src/main/java/com/newrelic/agent/util/AgentCollectionFactory.java \
   newrelic-agent/src/main/java11/com/newrelic/agent/util/
```

**File Organization:**
```
newrelic-agent/src/main/
├── java/                                    (Java 8 - base)
│   └── com/newrelic/agent/util/
│       ├── AgentCollectionFactory.java      (delegates to Caffeine2)
│       └── Caffeine2CollectionFactory.java
└── java11/                                  (Java 11 - versioned)
    └── com/newrelic/agent/util/
        ├── AgentCollectionFactory.java      (delegates to Caffeine3)
        └── Caffeine3CollectionFactory.java
```

### Step 5: Update JAR Task to Include Java 11 Classes

Find your main JAR task (likely `relocatedShadowJar` or similar) and add:

```gradle
task relocatedShadowJar(type: ShadowJar) {
    // ... existing configuration ...

    // Include Java 11 classes in Multi-Release JAR structure
    into('META-INF/versions/11') {
        from sourceSets.java11.output
    }

    // Ensure Java 11 compilation happens before JAR creation
    dependsOn compileJava11Java

    // ... rest of configuration ...
}
```

**Result JAR Structure:**
```
newrelic.jar:
├── com/newrelic/agent/util/
│   ├── AgentCollectionFactory.class           (Java 8)
│   └── Caffeine2CollectionFactory.class       (Java 8)
├── com/newrelic/agent/deps/caffeine2/...      (Caffeine 2.9.3 shaded)
├── com/newrelic/agent/deps/caffeine3/...      (Caffeine 3.2.3 shaded)
└── META-INF/
    ├── MANIFEST.MF                            (Multi-Release: true)
    └── versions/
        └── 11/
            └── com/newrelic/agent/util/
                ├── AgentCollectionFactory.class       (Java 11)
                └── Caffeine3CollectionFactory.class   (Java 11)
```

### Step 6: Update AgentCollectionFactory for Each Source Set

**Java 8 version** (`src/main/java/.../AgentCollectionFactory.java`):
```java
private static CollectionFactory selectCaffeineImplementation() {
    // Java 8 version always uses Caffeine 2
    return new Caffeine2CollectionFactory();
}
```

**Java 11 version** (`src/main/java11/.../AgentCollectionFactory.java`):
```java
private static CollectionFactory selectCaffeineImplementation() {
    // Java 11+ version always uses Caffeine 3
    return new Caffeine3CollectionFactory();
}
```

**Why this works:**
- On Java 8-10: JVM loads the base `AgentCollectionFactory` → uses Caffeine2
- On Java 11+: JVM loads from `META-INF/versions/11/` → uses Caffeine3
- No runtime version checking needed - the JVM handles it automatically!

### How It Works:

**Gradle File Structure:**
```
build.gradle:
  0. sourceSets { java11 { ... } }
  1. configurations { caffeine2, caffeine3 }
  2. createCaffeineShadingTask() method definition
  3. createCaffeineShadingTask('2') and ('3') calls  ← Tasks defined here
  4. dependencies {
       compileOnly files(shadeCaffeine2Jar.outputs.files)
       java11CompileOnly files(shadeCaffeine2Jar.outputs.files)
       java11CompileOnly files(shadeCaffeine3Jar.outputs.files)
     }
  5. compileJava { dependsOn ... }
  6. compileJava11Java { dependsOn ... }
```

**Build Execution Order:**
1. `shadeCaffeine2Jar` task runs → creates `build/caffeine-shaded/caffeine2-shaded.jar`
2. `shadeCaffeine3Jar` task runs → creates `build/caffeine-shaded/caffeine3-shaded.jar`
3. `compileJava` task runs (Java 8) → compiles base classes with Caffeine2
4. `compileJava11Java` task runs (Java 11) → compiles versioned classes with Caffeine3
5. JAR task packages both into Multi-Release JAR structure

**Runtime Behavior:**
- Java 8-10: Loads `AgentCollectionFactory` from base → uses Caffeine 2.9.3
- Java 11+: Loads `AgentCollectionFactory` from `META-INF/versions/11/` → uses Caffeine 3.2.3

### Step 7: IntelliJ IDEA Configuration (Optional but Recommended)

To help IntelliJ resolve the shaded packages and eliminate red squiggles:

#### Initial Build:
```bash
# Build shaded JARs
./gradlew :newrelic-agent:shadeCaffeine2Jar :newrelic-agent:shadeCaffeine3Jar

# Compile both source sets
./gradlew :newrelic-agent:compileJava :newrelic-agent:compileJava11Java
```

#### Configure IntelliJ:

**Option A: Automatic (Gradle Sync)**
1. Click **Gradle** tab in IntelliJ
2. Right-click on **newrelic-java-agent** → **Reload Gradle Project**
3. IntelliJ should automatically pick up the `compileOnly files(...)` dependencies

**Option B: Manual (If Option A doesn't work)**
1. Open **File → Project Structure** (or press `⌘;` on Mac / `Ctrl+Alt+Shift+S` on Windows)
2. Navigate to **Modules → newrelic-agent → Dependencies**
3. Click **+** (Add) → **JARs or directories**
4. Navigate to and select:
   - `newrelic-agent/build/caffeine-shaded/caffeine2-shaded.jar`
   - `newrelic-agent/build/caffeine-shaded/caffeine3-shaded.jar`
5. For each JAR, set **Scope** to **Provided**
6. Click **Apply** → **OK**

#### Verification:

Open `Caffeine2CollectionFactory.java` or `Caffeine3CollectionFactory.java` and verify:
- ✅ No red squiggles on imports
- ✅ Ctrl+Click (Cmd+Click) on `Caffeine` navigates to the class
- ✅ Code completion works for Caffeine API

#### Troubleshooting:

**Problem: "Duplicate class found" error for AgentCollectionFactory**
- **Cause:** IntelliJ sees the same class in both `src/main/java/` and `src/main/java11/`
- **Solution:** Reload Gradle Project (see Option A above). Gradle will configure IntelliJ to understand these are separate source sets.

**Problem: "Cannot resolve CollectionFactory" in java11 source set**
- **Cause:** The `java11` source set doesn't inherit dependencies from `main`
- **Solution:** Ensure you added this to `build.gradle` (right after the main `configurations` block):
  ```gradle
  configurations {
      java11Implementation.extendsFrom(implementation)
      java11CompileOnly.extendsFrom(compileOnly)
  }
  ```
  Then reload Gradle Project.

**Problem: Shaded package imports show red squiggles**
- **Cause:** Shaded JARs haven't been built yet or IntelliJ hasn't picked them up
- **Solution:**
  1. Build the shaded JARs: `./gradlew :newrelic-agent:shadeCaffeine2Jar :newrelic-agent:shadeCaffeine3Jar`
  2. Reload Gradle Project
  3. If still not working, try Option B (Manual) above

#### Note:
If you clean your build (`./gradlew clean`), the shaded JARs will be deleted. Re-run the shading tasks to recreate them:
```bash
./gradlew :newrelic-agent:shadeCaffeine2Jar :newrelic-agent:shadeCaffeine3Jar
```

---

## Phase 0b: No Runtime Version Checking Needed! ✅

**With Multi-Release JAR, we don't need `JavaVersionUtils` for version detection.**

The JVM automatically selects the correct class version:
- Java 8-10: Loads `AgentCollectionFactory` from base → always creates `Caffeine2CollectionFactory`
- Java 11+: Loads `AgentCollectionFactory` from `META-INF/versions/11/` → always creates `Caffeine3CollectionFactory`

**What to do:**

Remove any version checking logic from both versions of `AgentCollectionFactory`:

```java
// Java 8 version (src/main/java/.../AgentCollectionFactory.java)
private static final CollectionFactory DELEGATE = new Caffeine2CollectionFactory();
// No if/else needed!

// Java 11 version (src/main/java11/.../AgentCollectionFactory.java)
private static final CollectionFactory DELEGATE = new Caffeine3CollectionFactory();
// No if/else needed!
```

The JVM does the version selection for you by choosing which class file to load.

---

## Phase 1: Update CollectionFactory Interface

### File: `agent-bridge/src/main/java/com/newrelic/agent/bridge/CollectionFactory.java`

Add these new methods to the existing interface:

```java
package com.newrelic.agent.bridge;

import java.util.Map;
import java.util.function.Function;

/**
 * Allows instrumentation and bridge API implementations to use collections from third party libraries without
 * depending directly on them.
 */
public interface CollectionFactory {

    // ========================================================================
    // EXISTING METHODS (keep these)
    // ========================================================================

    <K, V> Map<K, V> createConcurrentWeakKeyedMap();

    <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds);

    <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize);

    <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader);

    // ========================================================================
    // NEW METHODS (add these)
    // ========================================================================

    /**
     * Create a loading cache that computes values on demand using the provided loader function.
     * This is the most common caching pattern used throughout the agent.
     *
     * @param <K>    key type
     * @param <V>    value type
     * @param loader the function to compute values for cache misses
     * @return a function that caches results
     */
    <K, V> Function<K, V> createLoadingCache(Function<K, V> loader);

    /**
     * Create a cache with weak keys and a maximum size.
     * Used for caching with automatic cleanup when keys are garbage collected.
     *
     * @param <K>     key type
     * @param <V>     value type
     * @param maxSize maximum number of entries before eviction
     * @return a map-backed cache with weak keys and size limit
     */
    <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize);

    /**
     * Create a cache with weak keys and initial capacity.
     * Used when weak key cleanup is needed with a known initial size.
     *
     * @param <K>             key type
     * @param <V>             value type
     * @param initialCapacity initial capacity to pre-allocate
     * @return a map-backed cache with weak keys
     */
    <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity);

    /**
     * Create a cache with weak keys, initial capacity, and maximum size.
     * Combines weak key cleanup with size bounds and initial capacity.
     *
     * @param <K>             key type
     * @param <V>             value type
     * @param initialCapacity initial capacity to pre-allocate
     * @param maxSize         maximum number of entries before eviction
     * @return a map-backed cache with weak keys, initial capacity, and size limit
     */
    <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize);

    /**
     * Create a cache with initial capacity only.
     * Used for simple caching without expiration or weak keys.
     *
     * @param <K>             key type
     * @param <V>             value type
     * @param initialCapacity initial capacity to pre-allocate
     * @return a map-backed cache
     */
    <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity);

    /**
     * Create a loading cache with weak keys and initial capacity.
     * Combines weak key cleanup with automatic value loading.
     *
     * @param <K>             key type
     * @param <V>             value type
     * @param initialCapacity initial capacity to pre-allocate
     * @param loader          the function to compute values for cache misses
     * @return a function that caches results with weak keys
     */
    <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader);
}
```

---

## Phase 2: Implement New Methods in Caffeine2CollectionFactory

### File: `newrelic-agent/src/main/java/com/newrelic/agent/util/Caffeine2CollectionFactory.java`

Add these implementations to the existing class:

```java
package com.newrelic.agent.util;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Cache;
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.LoadingCache;
import com.newrelic.agent.bridge.CollectionFactory;

/**
 * CollectionFactory implementation using Caffeine 2.9.3.
 * Used for Java 8-10.
 */
public class Caffeine2CollectionFactory implements CollectionFactory {

    // ========================================================================
    // EXISTING METHODS (already implemented)
    // ========================================================================

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(32)
                .weakKeys()
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(32)
                .expireAfterWrite(ageInSeconds, TimeUnit.SECONDS)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .expireAfterAccess(ageInSeconds, TimeUnit.SECONDS)
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    // ========================================================================
    // NEW METHODS (add these implementations)
    // ========================================================================

    @Override
    public <K, V> Function<K, V> createLoadingCache(Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .weakKeys()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }
}
```

---

## Phase 3: Implement New Methods in Caffeine3CollectionFactory

### File: `newrelic-agent/src/main/java/com/newrelic/agent/util/Caffeine3CollectionFactory.java`

Add these implementations (note: Duration instead of TimeUnit):

```java
package com.newrelic.agent.util;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.Cache;
import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.LoadingCache;
import com.newrelic.agent.bridge.CollectionFactory;

/**
 * CollectionFactory implementation using Caffeine 3.2.3.
 * Used for Java 11+. This version uses VarHandle instead of sun.misc.Unsafe.
 */
public class Caffeine3CollectionFactory implements CollectionFactory {

    // ========================================================================
    // EXISTING METHODS (already implemented)
    // ========================================================================

    @Override
    public <K, V> Map<K, V> createConcurrentWeakKeyedMap() {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(32)
                .weakKeys()
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        // Note: Caffeine 3.x uses Duration instead of TimeUnit
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(32)
                .expireAfterWrite(Duration.ofSeconds(ageInSeconds))
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        // Note: Caffeine 3.x uses Duration instead of TimeUnit
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .expireAfterAccess(Duration.ofSeconds(ageInSeconds))
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    // ========================================================================
    // NEW METHODS (add these implementations)
    // ========================================================================

    @Override
    public <K, V> Function<K, V> createLoadingCache(Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .weakKeys()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .maximumSize(maxSize)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .executor(Runnable::run)
                .build();
        return cache.asMap();
    }

    @Override
    public <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader) {
        LoadingCache<K, V> cache = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .weakKeys()
                .executor(Runnable::run)
                .build(loader::apply);
        return cache::get;
    }
}
```

---

## Phase 4: Example Refactoring - EmbeddedJarFilesImpl.java

### BEFORE: Direct Caffeine Usage

**File:** `newrelic-agent/src/main/java/com/newrelic/bootstrap/EmbeddedJarFilesImpl.java`

```java
package com.newrelic.bootstrap;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EmbeddedJarFilesImpl implements EmbeddedJarFiles {

    /**
     * A cache that extracts embedded JAR files to temp files on demand.
     * Direct Caffeine usage - will only use Caffeine 2 (with Unsafe)
     */
    private final LoadingCache<String, File> embeddedAgentJarFiles =
        Caffeine.newBuilder()
            .executor(Runnable::run)
            .build(new CacheLoader<String, File>() {
                @Override
                public File load(String jarNameWithoutExtension) throws IOException {
                    InputStream jarStream = EmbeddedJarFilesImpl.class.getClassLoader()
                        .getResourceAsStream(jarNameWithoutExtension + ".jar");

                    if (jarStream == null) {
                        throw new FileNotFoundException(jarNameWithoutExtension + ".jar");
                    }

                    File file = File.createTempFile(jarNameWithoutExtension, ".jar",
                                                    BootstrapLoader.getTempDir());
                    file.deleteOnExit();

                    try (OutputStream out = new FileOutputStream(file)) {
                        BootstrapLoader.copy(jarStream, out, 8096, true);
                        return file;
                    }
                }
            });

    @Override
    public File getJarFileInAgent(String jarNameWithoutExtension) throws IOException {
        return embeddedAgentJarFiles.get(jarNameWithoutExtension);
    }
}
```

---

### AFTER: Using AgentCollectionFactory

```java
package com.newrelic.bootstrap;

import com.newrelic.agent.bridge.AgentBridge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.function.Function;

public class EmbeddedJarFilesImpl implements EmbeddedJarFiles {

    /**
     * A cache that extracts embedded JAR files to temp files on demand.
     * Uses AgentCollectionFactory - automatically selects correct Caffeine version:
     * - Java 8-10: Uses Caffeine 2.9.3
     * - Java 11+:  Uses Caffeine 3.2.3 (no Unsafe)
     */
    private final Function<String, File> embeddedAgentJarFiles =
        AgentBridge.collectionFactory.createLoadingCache(this::loadJarFile);

    /**
     * Extracts an embedded JAR file to a temp file.
     */
    private File loadJarFile(String jarNameWithoutExtension) {
        try {
            InputStream jarStream = EmbeddedJarFilesImpl.class.getClassLoader()
                .getResourceAsStream(jarNameWithoutExtension + ".jar");

            if (jarStream == null) {
                throw new FileNotFoundException(jarNameWithoutExtension + ".jar");
            }

            File file = File.createTempFile(jarNameWithoutExtension, ".jar",
                                            BootstrapLoader.getTempDir());
            file.deleteOnExit();

            try (OutputStream out = new FileOutputStream(file)) {
                BootstrapLoader.copy(jarStream, out, 8096, true);
                return file;
            }
        } catch (IOException e) {
            // Wrap checked exception since Function can't throw checked exceptions
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public File getJarFileInAgent(String jarNameWithoutExtension) throws IOException {
        try {
            return embeddedAgentJarFiles.apply(jarNameWithoutExtension);
        } catch (UncheckedIOException e) {
            // Unwrap and rethrow as checked IOException
            throw (IOException) e.getCause();
        }
    }
}
```

### Key Changes:

1. **Imports Changed:**
   - ❌ Removed: `import com.github.benmanes.caffeine.cache.*`
   - ✅ Added: `import com.newrelic.agent.bridge.AgentBridge`
   - ✅ Added: `import java.io.UncheckedIOException` (for exception wrapping)
   - ✅ Added: `import java.util.function.Function`

2. **Field Type Changed:**
   - ❌ `LoadingCache<String, File> embeddedAgentJarFiles`
   - ✅ `Function<String, File> embeddedAgentJarFiles`

3. **Construction Changed:**
   - ❌ `Caffeine.newBuilder().build(new CacheLoader<>() {...})`
   - ✅ `AgentBridge.collectionFactory.createLoadingCache(this::loadJarFile)`
   - Extracts loader logic to separate method for clarity

4. **Usage Changed:**
   - ❌ `embeddedAgentJarFiles.get(key)` (automatically propagates checked exceptions)
   - ✅ `embeddedAgentJarFiles.apply(key)` with exception wrapping/unwrapping

5. **Exception Handling:**
   - Since `Function` can't throw checked exceptions, we wrap `IOException` → `UncheckedIOException`
   - Unwrap at the call site to maintain the original method signature

### Why This Pattern?

The `CacheLoader` interface allows checked exceptions, but `Function` doesn't. To maintain the same API (`getJarFileInAgent` throws `IOException`), we:
1. Wrap `IOException` in `UncheckedIOException` inside the loader
2. Unwrap it at the call site
3. This preserves the original exception semantics while using standard Java interfaces

---

## Phase 5: Files Requiring Updates

### Category 1: Direct Replacement (1 file)

These can use existing factory methods:

| File | Method to Use | Complexity |
|------|---------------|------------|
| **EmbeddedJarFilesImpl.java** | `createLoadingCache()` | ⭐ Easy |

---

### Category 2: Use New Factory Methods (19 files)

These require the new factory methods we just added:

#### **Use `createLoadingCache(Function)`** (11 files)

| File | Location | Current Pattern |
|------|----------|-----------------|
| EmbeddedJarFilesImpl.java | `/newrelic-agent/src/main/java/com/newrelic/bootstrap/` | `Caffeine.newBuilder().build(loader)` |
| MetricNameFormats.java | `/newrelic-agent/src/main/java/com/newrelic/agent/tracers/metricname/` | `Caffeine.newBuilder().build()` + `.get(key, loader)` |
| ThreadStateSampler.java | `/newrelic-agent/src/main/java/com/newrelic/agent/threads/` | `Caffeine.newBuilder().expireAfterAccess(...).build(loader)` |
| TransactionProfile.java | `/newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/` | `Caffeine.newBuilder().build(loader)` (2 caches) |
| TransactionProfileSessionImpl.java | `/newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/` | `Caffeine.newBuilder().build(loader)` (2 caches) |
| Profile.java | `/newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/` | `Caffeine.newBuilder().build(loader)` (2 caches) |
| DiscoveryProfile.java | `/newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/` | `Caffeine.newBuilder().build(loader)` |
| DefaultDestinationPredicate.java | `/newrelic-agent/src/main/java/com/newrelic/agent/attributes/` | `Caffeine.newBuilder().maximumSize(200).build(loader)` |
| ThreadService.java | `/newrelic-agent/src/main/java/com/newrelic/agent/` | `Caffeine.newBuilder().expireAfterAccess(5 min).build()` |
| TransactionEventsService.java | `/newrelic-agent/src/main/java/com/newrelic/agent/service/analytics/` | `Caffeine.newBuilder().maximumSize().expireAfterAccess().build(loader)` |
| LogSenderServiceImpl.java | `/newrelic-agent/src/main/java/com/newrelic/agent/service/logging/` | `Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(70s).build(loader)` |
| InsightsServiceImpl.java | `/newrelic-agent/src/main/java/com/newrelic/agent/service/analytics/` | `Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(70s).build(loader)` |

#### **Use `createCacheWithWeakKeysAndSize(int)`** (1 file)

| File | Location | Current Pattern |
|------|----------|-----------------|
| CachingDatabaseStatementParser.java | `/newrelic-agent/src/main/java/com/newrelic/agent/database/` | `Caffeine.newBuilder().maximumSize(1000).weakKeys().build()` |

#### **Use `createWeakKeyedCacheWithInitialCapacity(int)`** (1 file)

| File | Location | Current Pattern |
|------|----------|-----------------|
| ExtensionHolderFactoryImpl.java | `/newrelic-agent/src/main/java/com/newrelic/agent/instrumentation/weaver/extension/` | `Caffeine.newBuilder().initialCapacity(32).weakKeys().build()` |

#### **Use `createCacheWithWeakKeysInitialCapacityAndSize(int, int)`** (1 file)

| File | Location | Current Pattern |
|------|----------|-----------------|
| WeavePackageManager.java | `/newrelic-weaver/src/main/java/com/newrelic/weave/weavepackage/` | `Caffeine.newBuilder().weakKeys().initialCapacity().maximumSize().build()` (3 caches) |

#### **Use `createWeakKeyedLoadingCacheWithInitialCapacity(int, Function)`** (1 file)

| File | Location | Current Pattern |
|------|----------|-----------------|
| CloudAccountInfoCache.java | `/newrelic-agent/src/main/java/com/newrelic/agent/cloud/` | `Caffeine.newBuilder().initialCapacity(4).weakKeys().build(loader)` |

#### **Use `createCacheWithInitialCapacity(int)`** (1 file)

| File | Location | Current Pattern |
|------|----------|-----------------|
| BoundedConcurrentCache.java | `/newrelic-agent/src/main/java/com/newrelic/agent/sql/` | `Caffeine.newBuilder().initialCapacity(16).build()` |

#### **Requires Additional Work** (3 files - need RemovalListener support)

| File | Location | Why Complex | Solution |
|------|----------|-------------|----------|
| **AsyncTransactionService.java** | `/newrelic-agent/src/main/java/com/newrelic/agent/service/async/` | Uses `expireAfterWrite()` + `removalListener()` | See Phase 6 below |
| **TimedTokenSet.java** | `/newrelic-agent/src/main/java/com/newrelic/agent/` | Uses `expireAfterAccess()` + `removalListener()` + complex removal logic | See Phase 6 below |
| **TransactionProfileSessionImpl.java** | `/newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/` | One cache needs `expireAfterAccess()` + loader | Use `createAccessTimeBasedCache()` with loader |

---

## Phase 6: Adding RemovalListener Support (Optional - 3 Files)

### Overview

Three files use Caffeine's `RemovalListener` to execute business logic when cache entries are removed. For example, `TimedTokenSet.java` uses it to:
- Track timeout counters
- Update token and transaction state
- Spawn cleanup work on separate threads

**Complexity:** Moderate (~3-4 hours)
**Approach:** Create a bridge abstraction for RemovalListener

### Step 1: Add Bridge RemovalListener Interface

**File:** `agent-bridge/src/main/java/com/newrelic/agent/bridge/CacheRemovalListener.java`

```java
package com.newrelic.agent.bridge;

/**
 * Listener for cache entry removals. Abstracts away the specific cache implementation.
 */
@FunctionalInterface
public interface CacheRemovalListener<K, V> {
    /**
     * Called when an entry is removed from the cache.
     *
     * @param key    the removed key (may be null)
     * @param value  the removed value (may be null)
     * @param reason why the entry was removed
     */
    void onRemoval(K key, V value, RemovalReason reason);
}
```

**File:** `agent-bridge/src/main/java/com/newrelic/agent/bridge/RemovalReason.java`

```java
package com.newrelic.agent.bridge;

/**
 * The reason why a cache entry was removed.
 */
public enum RemovalReason {
    /**
     * The entry was removed due to expiration (time-based eviction).
     */
    EXPIRED,

    /**
     * The entry was manually removed (invalidate, remove, or clear).
     */
    EXPLICIT,

    /**
     * The entry was removed due to size-based eviction.
     */
    SIZE,

    /**
     * The entry was replaced with a new value.
     */
    REPLACED,

    /**
     * The entry's key or value was garbage collected (weak/soft references).
     */
    COLLECTED
}
```

### Step 2: Add Factory Methods to CollectionFactory

**File:** `agent-bridge/src/main/java/com/newrelic/agent/bridge/CollectionFactory.java`

Add these methods to the existing interface:

```java
/**
 * Create a cache with time-based eviction (write) and removal listener.
 * Used for tracking when cache entries expire or are removed.
 *
 * @param <K>             key type
 * @param <V>             value type
 * @param ageInSeconds    time after write before expiration
 * @param initialCapacity initial capacity
 * @param listener        called when entries are removed
 * @return a map-backed cache with expiration and removal tracking
 */
<K, V> Map<K, V> createCacheWithWriteExpirationAndRemovalListener(
        long ageInSeconds,
        int initialCapacity,
        CacheRemovalListener<K, V> listener);

/**
 * Create a cache with time-based eviction (access) and removal listener.
 * Used for tracking when cache entries expire or are removed.
 *
 * @param <K>             key type
 * @param <V>             value type
 * @param ageInSeconds    time after access before expiration
 * @param initialCapacity initial capacity
 * @param listener        called when entries are removed
 * @return a map-backed cache with expiration and removal tracking
 */
<K, V> Map<K, V> createCacheWithAccessExpirationAndRemovalListener(
        long ageInSeconds,
        int initialCapacity,
        CacheRemovalListener<K, V> listener);
```

### Step 3: Implement in Caffeine2CollectionFactory

**File:** `newrelic-agent/src/main/java/com/newrelic/agent/util/Caffeine2CollectionFactory.java`

```java
@Override
public <K, V> Map<K, V> createCacheWithWriteExpirationAndRemovalListener(
        long ageInSeconds,
        int initialCapacity,
        CacheRemovalListener<K, V> listener) {
    Cache<K, V> cache = Caffeine.newBuilder()
            .initialCapacity(initialCapacity)
            .expireAfterWrite(ageInSeconds, TimeUnit.SECONDS)
            .executor(Runnable::run)
            .removalListener((key, value, cause) -> {
                listener.onRemoval(key, value, convertRemovalCause(cause));
            })
            .build();
    return cache.asMap();
}

@Override
public <K, V> Map<K, V> createCacheWithAccessExpirationAndRemovalListener(
        long ageInSeconds,
        int initialCapacity,
        CacheRemovalListener<K, V> listener) {
    Cache<K, V> cache = Caffeine.newBuilder()
            .initialCapacity(initialCapacity)
            .expireAfterAccess(ageInSeconds, TimeUnit.SECONDS)
            .executor(Runnable::run)
            .removalListener((key, value, cause) -> {
                listener.onRemoval(key, value, convertRemovalCause(cause));
            })
            .build();
    return cache.asMap();
}

private RemovalReason convertRemovalCause(
        com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.RemovalCause cause) {
    switch (cause) {
        case EXPIRED: return RemovalReason.EXPIRED;
        case EXPLICIT: return RemovalReason.EXPLICIT;
        case SIZE: return RemovalReason.SIZE;
        case REPLACED: return RemovalReason.REPLACED;
        case COLLECTED: return RemovalReason.COLLECTED;
        default: return RemovalReason.EXPLICIT;
    }
}
```

### Step 4: Implement in Caffeine3CollectionFactory

**File:** `newrelic-agent/src/main/java/com/newrelic/agent/util/Caffeine3CollectionFactory.java`

```java
@Override
public <K, V> Map<K, V> createCacheWithWriteExpirationAndRemovalListener(
        long ageInSeconds,
        int initialCapacity,
        CacheRemovalListener<K, V> listener) {
    Cache<K, V> cache = Caffeine.newBuilder()
            .initialCapacity(initialCapacity)
            .expireAfterWrite(Duration.ofSeconds(ageInSeconds))
            .executor(Runnable::run)
            .removalListener((key, value, cause) -> {
                listener.onRemoval(key, value, convertRemovalCause(cause));
            })
            .build();
    return cache.asMap();
}

@Override
public <K, V> Map<K, V> createCacheWithAccessExpirationAndRemovalListener(
        long ageInSeconds,
        int initialCapacity,
        CacheRemovalListener<K, V> listener) {
    Cache<K, V> cache = Caffeine.newBuilder()
            .initialCapacity(initialCapacity)
            .expireAfterAccess(Duration.ofSeconds(ageInSeconds))
            .executor(Runnable::run)
            .removalListener((key, value, cause) -> {
                listener.onRemoval(key, value, convertRemovalCause(cause));
            })
            .build();
    return cache.asMap();
}

private RemovalReason convertRemovalCause(
        com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.RemovalCause cause) {
    switch (cause) {
        case EXPIRED: return RemovalReason.EXPIRED;
        case EXPLICIT: return RemovalReason.EXPLICIT;
        case SIZE: return RemovalReason.SIZE;
        case REPLACED: return RemovalReason.REPLACED;
        case COLLECTED: return RemovalReason.COLLECTED;
        default: return RemovalReason.EXPLICIT;
    }
}
```

### Step 5: Refactor TimedTokenSet.java Example

**BEFORE:**
```java
import com.github.benmanes.caffeine.cache.*;

activeTokens = Caffeine.newBuilder()
    .initialCapacity(8)
    .expireAfterAccess(timeOutMilli, TimeUnit.MILLISECONDS)
    .executor(Runnable::run)
    .removalListener(new RemovalListener<TokenImpl, TokenImpl>() {
        @Override
        public void onRemoval(TokenImpl token, TokenImpl value, RemovalCause cause) {
            if (cause == RemovalCause.EXPIRED) {
                // Handle timeout logic...
            } else if (cause == RemovalCause.EXPLICIT) {
                // Handle explicit removal...
            }
        }
    }).build();
```

**AFTER:**
```java
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.RemovalReason;

long timeOutSeconds = TimeUnit.MILLISECONDS.toSeconds(timeOutMilli);

activeTokens = AgentBridge.collectionFactory.createCacheWithAccessExpirationAndRemovalListener(
    timeOutSeconds,
    8,
    (token, value, reason) -> {
        Transaction tx = token.getTransaction().getTransactionIfExists();

        try {
            if (reason == RemovalReason.EXPIRED) {
                // Handle timeout logic...
                Agent.LOG.log(Level.FINEST, "Timing out token {0} on transaction {1}", token, tx);
                timedOutTokens.incrementAndGet();
                token.setTruncated();
                if (tx != null) {
                    tx.setTimeoutCause(TimeoutCause.TOKEN);
                }
            } else if (reason == RemovalReason.EXPLICIT) {
                // Handle explicit removal...
                Agent.LOG.log(Level.FINEST, "Expiring token {0} on transaction {1}", token, tx);
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, "Token {0} on transaction {1} threw exception: {2}", token, tx, e);
        } finally {
            expirationService.expireToken(token::markExpired);
        }
    });
```

### Key Changes:
1. ✅ Remove Caffeine imports
2. ✅ Add AgentBridge and RemovalReason imports
3. ✅ Use factory method with lambda instead of anonymous inner class
4. ✅ Replace `RemovalCause` with `RemovalReason`
5. ✅ Convert time units if needed (milliseconds → seconds)

### Files to Update with This Pattern:
1. **TimedTokenSet.java** - Uses `expireAfterAccess` + listener (line 39-77)
2. **AsyncTransactionService.java** - Uses `expireAfterWrite` + listener

---

## Summary: Implementation Order

### **Step 1: Update Interfaces and Factories** ✅
1. Add new methods to `CollectionFactory.java`
2. Implement in `Caffeine2CollectionFactory.java`
3. Implement in `Caffeine3CollectionFactory.java`
4. Build and verify compilation

### **Step 2: Refactor Simple Cases (Proof of Concept)** ✅
5. **EmbeddedJarFilesImpl.java** - Easiest example, proves the pattern works

### **Step 3: Refactor Standard Files (17 files)** 🔄
6. **CachingDatabaseStatementParser.java** - `createCacheWithWeakKeysAndSize(1000)`
7. **ExtensionHolderFactoryImpl.java** - `createWeakKeyedCacheWithInitialCapacity(32)`
8. **CloudAccountInfoCache.java** - `createWeakKeyedLoadingCacheWithInitialCapacity(4, loader)`
9. **BoundedConcurrentCache.java** - `createCacheWithInitialCapacity(16)`
10. **WeavePackageManager.java** - `createCacheWithWeakKeysInitialCapacityAndSize(...)` (3 caches)
11. **EmbeddedJarFilesImpl.java** - `createLoadingCache(loader)`
12. **MetricNameFormats.java** - `createLoadingCache(...)` with `.apply(key)`
13. **All Profile files** (4 files) - `createLoadingCache(loader)`
14. **TransactionEventsService.java** - `createLoadingCache(...)` or `createAccessTimeBasedCache(...)`
15. **LogSenderServiceImpl.java** - `createAccessTimeBasedCache(70, 1000, identityLoader)`
16. **InsightsServiceImpl.java** - Same as LogSenderServiceImpl
17. **DefaultDestinationPredicate.java** - `memorize(this::isIncluded, 200)`
18. **ThreadStateSampler.java** - `createAccessTimeBasedCache(180, initialCap, loader)`
19. **ThreadService.java** - `createAccessTimeBasedCache(300, initialCap, loader)`
20. **TransactionProfileSessionImpl.java** - `createAccessTimeBasedCache(...)` with loader

### **Step 4: Add RemovalListener Support (Optional - 2 files)** ⚠️
See **Phase 6** above for complete implementation details.

21. **AsyncTransactionService.java** - `createCacheWithWriteExpirationAndRemovalListener(...)`
22. **TimedTokenSet.java** - `createCacheWithAccessExpirationAndRemovalListener(...)`

---

## Testing Strategy

After each file refactoring:

1. **Compile:** `./gradlew :newrelic-agent:compileJava`
2. **Unit Test:** `./gradlew :newrelic-agent:test --tests *<ClassName>*`
3. **Integration Test on Java 8:** `./gradlew :newrelic-agent:test -Ptest8`
4. **Integration Test on Java 11:** `./gradlew :newrelic-agent:test -Ptest11`

---

## Quick Reference: Common Patterns

### Pattern 1: Simple Weak Keys Cache
```java
// BEFORE
Cache<K, V> cache = Caffeine.newBuilder().weakKeys().executor(Runnable::run).build();

// AFTER
Map<K, V> cache = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();
```

### Pattern 2: Loading Cache
```java
// BEFORE
LoadingCache<K, V> cache = Caffeine.newBuilder().executor(Runnable::run).build(this::loader);
V value = cache.get(key);

// AFTER
Function<K, V> cache = AgentBridge.collectionFactory.createLoadingCache(this::loader);
V value = cache.apply(key);
```

### Pattern 3: Weak Keys + Size Limit
```java
// BEFORE
Cache<K, V> cache = Caffeine.newBuilder().weakKeys().maximumSize(1000).executor(Runnable::run).build();

// AFTER
Map<K, V> cache = AgentBridge.collectionFactory.createCacheWithWeakKeysAndSize(1000);
```

### Pattern 4: Weak Keys + Initial Capacity
```java
// BEFORE
Cache<K, V> cache = Caffeine.newBuilder().initialCapacity(32).weakKeys().executor(Runnable::run).build();

// AFTER
Map<K, V> cache = AgentBridge.collectionFactory.createWeakKeyedCacheWithInitialCapacity(32);
```

---

## Files Summary

- **Total files to update:** 20
- **Easy (use existing methods):** 1 file (EmbeddedJarFilesImpl)
- **Medium (use new methods from Phase 1-3):** 17 files
- **Complex (needs RemovalListener - Phase 6):** 2 files (optional)
- **Estimated effort:**
  - Phases 1-3 (18 files): 2-3 days
  - Phase 6 RemovalListener (2 files): 3-4 hours additional (optional)

---

## Next Steps

### Required Work:
1. ✅ Review this guide
2. 🔄 **Phase 0**: Configure build.gradle with Multi-Release JAR and dual-shading
3. 🔄 **Phase 1**: Add new methods to CollectionFactory interface (6 new methods)
4. 🔄 **Phase 2**: Implement new methods in Caffeine2CollectionFactory
5. 🔄 **Phase 3**: Implement new methods in Caffeine3CollectionFactory
6. 🔄 **Phase 4**: Create java11 source set and move Caffeine3 classes
7. 🔄 **Phase 5**: Update both AgentCollectionFactory versions (no version checking needed!)
8. 🔄 **Phase 6**: Refactor EmbeddedJarFilesImpl (proof of concept)
9. 🔄 **Phase 7**: Systematically refactor remaining 17 files
10. ✅ Test on Java 8, 11, 17, 21
11. ✅ Verify no Unsafe warnings on Java 11+

### Optional Work:
12. ⚠️ **Phase 8** (Optional): Add RemovalListener support for AsyncTransactionService and TimedTokenSet
    - Estimated effort: 3-4 hours
    - Can be deferred if these files are low priority
    - See Phase 6 in the guide above for implementation details

Good luck! 🚀
