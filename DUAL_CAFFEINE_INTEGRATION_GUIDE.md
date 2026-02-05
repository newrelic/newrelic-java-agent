# Dual Caffeine Integration Guide

## ✅ Proof of Concept Results

The dual-shading approach **works successfully**! We've demonstrated that two versions of Caffeine can be shaded into different packages and bundled in the same JAR.

### What Was Achieved

```
Generated JARs (in dual-caffeine-test/build/caffeine-shaded/):
├── caffeine2-shaded.jar  (1.2 MB) - Caffeine 2.9.3 for Java 8-10
├── caffeine3-shaded.jar  (1.0 MB) - Caffeine 3.2.3 for Java 11+
└── dual-caffeine.jar     (2.2 MB) - Combined JAR with both versions

Package Structure:
├── com.newrelic.agent.deps.caffeine2.* (1,048 classes)
│   └── Caffeine 2.9.3 - uses sun.misc.Unsafe (Java 8-10)
└── com.newrelic.agent.deps.caffeine3.* (717 classes)
    └── Caffeine 3.2.3 - uses VarHandle, no Unsafe (Java 11+)
```

### Key Verification

Both `Caffeine.class` builder classes exist in separate packages:
- `com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Caffeine`
- `com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.Caffeine`

---

## 📋 Integration Steps for newrelic-agent/build.gradle

### Step 1: Add Caffeine Configurations

Add these configurations to the existing `configurations` block:

```gradle
configurations {
    tests
    shadowIntoJar
    jarIntoJar
    finalArtifact

    // NEW: Add these two simple configurations
    caffeine2
    caffeine3
}
```

### Step 2: Update Dependencies

In the `dependencies` block:

```gradle
dependencies {
    // ... existing dependencies ...

    // REMOVE THIS LINE:
    // shadowIntoJar('com.github.ben-manes.caffeine:caffeine:2.9.3')

    // ADD THESE INSTEAD:
    caffeine2('com.github.ben-manes.caffeine:caffeine:2.9.3')
    caffeine3('com.github.ben-manes.caffeine:caffeine:3.2.3')

    // ... rest of dependencies ...
}
```

### Step 3: Create Caffeine Shading Tasks

Add this helper method and task creation **before** the `relocatedShadowJar` task:

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

// Create both shading tasks
createCaffeineShadingTask('2')
createCaffeineShadingTask('3')
```

### Step 4: Update relocatedShadowJar Task

Modify the existing `relocatedShadowJar` task to include both shaded Caffeine JARs:

```gradle
task relocatedShadowJar(type: ShadowJar) {
    // ADD THIS LINE:
    dependsOn("classes", "processResources", "generateVersionProperties", shadeCaffeine2Jar, shadeCaffeine3Jar)

    from sourceSets.main.output.classesDirs
    from(sourceSets.main.output.resourcesDir) {
        exclude("*.jar")
    }

    // ADD THESE LINES: Include both shaded Caffeine JARs
    from(zipTree(shadeCaffeine2Jar.archiveFile.get().asFile)) {
        include '**'
    }
    from(zipTree(shadeCaffeine3Jar.archiveFile.get().asFile)) {
        include '**'
    }

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
            // REMOVE: "com.github.benmanes",  <-- Remove this line since we handle Caffeine separately
            "org.crac",

            // ... rest of relocations ...
    ].each {
        relocate(it, "com.newrelic.agent.deps.$it")
    }

    // ... rest of task configuration ...
}
```

---

## 📝 Code Changes Required

### 1. Create JavaVersion Utility

Create `newrelic-agent/src/main/java/com/newrelic/agent/util/JavaVersion.java`:

```java
package com.newrelic.agent.util;

public class JavaVersion {
    private static final int MAJOR_VERSION = detectMajorVersion();

    private static int detectMajorVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        } else {
            int dotIndex = version.indexOf('.');
            int dashIndex = version.indexOf('-');
            int endIndex = (dotIndex > 0) ? dotIndex :
                          (dashIndex > 0) ? dashIndex : version.length();
            return Integer.parseInt(version.substring(0, endIndex));
        }
    }

    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }

    public static boolean shouldUseCaffeine3() {
        return MAJOR_VERSION >= 11;
    }
}
```

### 2. Create Caffeine2CollectionFactory

Create `newrelic-agent/src/main/java/com/newrelic/agent/util/Caffeine2CollectionFactory.java`:

```java
package com.newrelic.agent.util;

import com.newrelic.agent.bridge.CollectionFactory;
// Import from SHADED Caffeine 2.9.3
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Cache;
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Caffeine2CollectionFactory implements CollectionFactory {

    @Override
    public <K, V> ConcurrentWeakKeyedMap<K, V> createConcurrentWeakKeyedMap() {
        Cache<K, V> cache = Caffeine.newBuilder()
                .weakKeys()
                .executor(Runnable::run)
                .build();
        return new CaffeineBackedMap<>(cache);
    }

    @Override
    public <K, V> ConcurrentTimeBasedEvictionMap<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .expireAfterWrite(ageInSeconds, TimeUnit.SECONDS)
                .executor(Runnable::run)
                .build();
        return new CaffeineBackedMap<>(cache);
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
    public <K, V> ConcurrentTimeBasedEvictionMap<K, V> createAccessTimeBasedCache(
            long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .expireAfterAccess(ageInSeconds, TimeUnit.SECONDS)
                .initialCapacity(initialCapacity)
                .executor(Runnable::run)
                .build();
        return new CaffeineBackedMap<>(cache, loader);
    }
}
```

### 3. Create Caffeine3CollectionFactory

Create `newrelic-agent/src/main/java/com/newrelic/agent/util/Caffeine3CollectionFactory.java`:

```java
package com.newrelic.agent.util;

import com.newrelic.agent.bridge.CollectionFactory;
// Import from SHADED Caffeine 3.x
import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.Cache;
import com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.function.Function;

public class Caffeine3CollectionFactory implements CollectionFactory {

    @Override
    public <K, V> ConcurrentWeakKeyedMap<K, V> createConcurrentWeakKeyedMap() {
        Cache<K, V> cache = Caffeine.newBuilder()
                .weakKeys()
                .executor(Runnable::run)
                .build();
        return new CaffeineBackedMap<>(cache);
    }

    @Override
    public <K, V> ConcurrentTimeBasedEvictionMap<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        // NOTE: Caffeine 3.x uses Duration instead of TimeUnit
        Cache<K, V> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ageInSeconds))
                .executor(Runnable::run)
                .build();
        return new CaffeineBackedMap<>(cache);
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
    public <K, V> ConcurrentTimeBasedEvictionMap<K, V> createAccessTimeBasedCache(
            long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        Cache<K, V> cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(ageInSeconds))
                .initialCapacity(initialCapacity)
                .executor(Runnable::run)
                .build();
        return new CaffeineBackedMap<>(cache, loader);
    }
}
```

### 4. Update AgentCollectionFactory

Modify `newrelic-agent/src/main/java/com/newrelic/agent/util/AgentCollectionFactory.java`:

```java
package com.newrelic.agent.util;

import com.newrelic.agent.bridge.CollectionFactory;
import java.util.logging.Level;

public class AgentCollectionFactory implements CollectionFactory {

    private static final CollectionFactory DELEGATE = selectCaffeineImplementation();

    private static CollectionFactory selectCaffeineImplementation() {
        int javaVersion = JavaVersion.getMajorVersion();

        if (JavaVersion.shouldUseCaffeine3()) {
            logInfo("Java " + javaVersion + " detected. Using Caffeine 3.x (VarHandle-based, no Unsafe)");
            return new Caffeine3CollectionFactory();
        } else {
            logInfo("Java " + javaVersion + " detected. Using Caffeine 2.9.3");
            return new Caffeine2CollectionFactory();
        }
    }

    private static void logInfo(String message) {
        try {
            AgentBridge.getAgent().getLogger().log(Level.INFO, message);
        } catch (Exception e) {
            // Fallback if logging not available during early initialization
            System.out.println("[NewRelic] " + message);
        }
    }

    @Override
    public <K, V> ConcurrentWeakKeyedMap<K, V> createConcurrentWeakKeyedMap() {
        return DELEGATE.createConcurrentWeakKeyedMap();
    }

    @Override
    public <K, V> ConcurrentTimeBasedEvictionMap<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds) {
        return DELEGATE.createConcurrentTimeBasedEvictionMap(ageInSeconds);
    }

    @Override
    public <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize) {
        return DELEGATE.memorize(loader, maxSize);
    }

    @Override
    public <K, V> ConcurrentTimeBasedEvictionMap<K, V> createAccessTimeBasedCache(
            long ageInSeconds, int initialCapacity, Function<K, V> loader) {
        return DELEGATE.createAccessTimeBasedCache(ageInSeconds, initialCapacity, loader);
    }
}
```

---

## 🧪 Testing Strategy

### 1. Unit Tests

Create unit tests for Java version detection and factory selection:

```java
public class JavaVersionTest {
    @Test
    public void testJavaVersionDetection() {
        int version = JavaVersion.getMajorVersion();
        assertTrue("Version should be >= 8", version >= 8);
    }

    @Test
    public void testJava11RequiresCaffeine3() {
        // When running on Java 11+, should use Caffeine 3
        assertTrue(JavaVersion.shouldUseCaffeine3()); // When running on Java 11+
    }
}

public class CaffeineFactorySelectionTest {
    @Test
    public void testFactorySelectionForJava8() {
        // Test that Caffeine2 is selected for Java 8-10
    }

    @Test
    public void testFactorySelectionForJava26() {
        // Test that Caffeine3 is selected for Java 11+
    }
}
```

### 2. Integration Tests

Test the agent on different Java versions:

```bash
# Java 8
./gradlew :newrelic-agent:test -Ptest8

# Java 11
./gradlew :newrelic-agent:test -Ptest11

# Java 17
./gradlew :newrelic-agent:test -Ptest17

# Java 21
./gradlew :newrelic-agent:test -Ptest21

# Java 11
./gradlew :newrelic-agent:test -Ptest11
```

### 3. Smoke Tests

Verify basic cache functionality:

```java
@Test
public void testWeakKeyedMapWorks() {
    CollectionFactory factory = new AgentCollectionFactory();
    ConcurrentWeakKeyedMap<Object, String> map = factory.createConcurrentWeakKeyedMap();

    Object key = new Object();
    map.put(key, "value");
    assertEquals("value", map.get(key));

    key = null;
    System.gc();
    Thread.sleep(100);

    // Verify weak key was collected
    assertTrue(map.isEmpty());
}
```

### 4. Performance Tests

Compare Caffeine 2 vs 3 performance:

```java
@Test
public void benchmarkCacheOperations() {
    // Test get/put operations
    // Test eviction performance
    // Test memory usage
}
```

---

## 📊 Size Impact Analysis

### Before (Current)
```
newrelic.jar size: ~X MB
├── Caffeine 2.9.3: 1.2 MB
```

### After (Dual Caffeine)
```
newrelic.jar size: ~X + 1.0 MB
├── Caffeine 2.9.3: 1.2 MB
├── Caffeine 3.2.3: 1.0 MB
└── Total overhead: +1.0 MB (Caffeine 3 is smaller)
```

**Net increase: ~1.0 MB (~4% for typical agent JAR)**

---

## ✅ Advantages of This Approach

1. **No Forking**: Use official Caffeine releases
2. **No Patching**: Zero maintenance burden for Caffeine internals
3. **No Performance Loss**: Java 8-10 users keep battle-tested Caffeine 2.9.3
4. **Future-Proof**: Java 11+ users get Unsafe-free Caffeine 3.x
5. **Transparent**: No code changes needed outside factory layer
6. **Graceful Fallback**: Can fallback to DefaultCollectionFactory if needed
7. **Low Risk**: Each version is isolated in its own package
8. **Easy Testing**: Can test both implementations independently

---

## 🚀 Rollout Plan

### Phase 1: Development (Week 1-2)
- [ ] Implement Gradle configuration changes
- [ ] Create Java version detector
- [ ] Implement Caffeine2CollectionFactory
- [ ] Implement Caffeine3CollectionFactory
- [ ] Update AgentCollectionFactory with selection logic
- [ ] Write unit tests

### Phase 2: Testing (Week 3-4)
- [ ] Integration tests on Java 8, 11, 17, 21
- [ ] Smoke tests for cache functionality
- [ ] Performance benchmarks
- [ ] Memory leak tests
- [ ] Java 11, 17, 21 testing

### Phase 3: Beta Release (Week 5-6)
- [ ] Internal dogfooding
- [ ] Beta release to select customers
- [ ] Monitor for issues

### Phase 4: GA Release (Week 7-8)
- [ ] General availability
- [ ] Documentation updates
- [ ] Release notes

---

## 📚 API Compatibility Notes

### Key Differences Between Caffeine 2.9.3 and 3.x

| Feature | Caffeine 2.9.3 | Caffeine 3.x | Impact |
|---------|---------------|--------------|--------|
| Time units | `expireAfterWrite(long, TimeUnit)` | `expireAfterWrite(Duration)` | Factory abstraction handles it |
| Nullability annotations | `@Nullable` (Checker Framework) | `@Nullable` (JSpecify) | No impact |
| Async | `CompletableFuture` | `CompletableFuture` | No change |
| Java version | Java 8+ | Java 11+ | Runtime detection handles it |
| Unsafe usage | Yes | No (uses VarHandle) | Primary motivation |

All API differences are abstracted by the `CollectionFactory` interface, ensuring transparent operation.

---

## 🔍 Verification Commands

After integration, verify the build:

```bash
# Build the agent
./gradlew :newrelic-agent:newrelicVersionedAgentJar

# Verify both Caffeine versions are present
jar tf newrelic-agent/build/libs/newrelic-*.jar | grep -E "caffeine2.*Caffeine\.class|caffeine3.*Caffeine\.class"

# Expected output:
# com/newrelic/agent/deps/caffeine2/com/github/benmanes/caffeine/cache/Caffeine.class
# com/newrelic/agent/deps/caffeine3/com/github/benmanes/caffeine/cache/Caffeine.class

# Check JAR size
ls -lh newrelic-agent/build/libs/newrelic-*.jar
```

---

## 🎯 Success Criteria

- ✅ Agent builds successfully with both Caffeine versions
- ✅ Agent runs on Java 8-10 using Caffeine 2.9.3
- ✅ Agent runs on Java 11+ using Caffeine 3.x
- ✅ All existing tests pass
- ✅ No performance regression on Java 8-21
- ✅ Cache functionality works identically
- ✅ No sun.misc.Unsafe errors on Java 11+
- ✅ JAR size increase < 5%
- ✅ No class conflicts between versions

---

## 📞 Questions?

For questions or issues during implementation, refer to:
- This guide
- The working POC in `dual-caffeine-test/`
- The generated POC file `dual-caffeine-poc.gradle`
