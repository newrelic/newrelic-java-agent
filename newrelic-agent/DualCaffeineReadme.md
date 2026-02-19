# Dual Caffeine Version Support

## Overview

This document explains the implementation of dual Caffeine support in the agent. This change
was required because of the complete removal of the "Unsafe" class in Java 26. Caffeine v3+
eliminated any calls to the Unsafe class, however Caffeine v2 will not receive a similar
update. Caffeine v3 only supports Java 11+, so Caffeine v2 is still required to support
Java 8 - 12.

- **Caffeine 2.9.3** - For Java 8-10 (uses `sun.misc.Unsafe`)
- **Caffeine 3.2.3** - For Java 11+ (uses `VarHandle`)

Both versions are shaded with relocated package names to avoid conflicts with user applications, 
and the agent automatically selects the appropriate version at runtime based on the Java version,
utilizing the CollectionFactory on the AgentBridge.

A couple of somewhat gross implementation details:
- Both versions of Caffeine are shaded into the Agent jar
- A multi-release jar is utilized in order to support loading the proper Caffeine versions at runtime
- The shaded package name for Caffeine is used in the `import` statements in the CollectionFactory
- The `newrelic-weaver` project now has a dependency on `agent-bridge`

#### Why not transition to a new cache provider??
- cache2k, ehcache - No support for weak keyed caches
- guava - suffers from the same `Unsafe` usage that Caffeine v2 does 
- "Roll your own" caches in vanilla Java - Extremely difficult and brittle to implement weak key caches
and max size caches with acceptable performance (although one was created for the weaver project)

### Multi-Release JAR Structure

The agent uses Java's Multi-Release JAR feature to provide different implementations based on the runtime Java version:

```
newrelic-agent.jar
├── com/newrelic/agent/util/
│   └── AgentCollectionFactory.class          # Java 8 version (delegates to Caffeine2)
└── META-INF/versions/11/
    └── com/newrelic/agent/util/
        └── AgentCollectionFactory.class      # Java 11 version (delegates to Caffeine3)
```

### Object Graph

```
AgentBridge (agent-bridge module)
  └─ collectionFactory: CollectionFactory (interface)
       │
       │ initialized at startup
       ▼
  AgentCollectionFactory (newrelic-agent module)
    - Java 8-10: Delegates to Caffeine2CollectionFactory
    - Java 11+:  Delegates to Caffeine3CollectionFactory
       │
       ├──────────────────────┬──────────────────────┐
       ▼                      ▼                      ▼
  Caffeine2Collection    Caffeine3Collection    DefaultCollection
  Factory                Factory                Factory
  (Java 8-10)            (Java 11+)             (Fallback - not used in normal Agent operation)
    - Shaded               - Shaded               - JDK Collections only
      Caffeine 2.9.3         Caffeine 3.2.3
    - sun.misc.Unsafe      - VarHandle
       │                      │
       └──────────────────────┘
                │
                │ all implement
                ▼
          CollectionFactory (interface)

            // Basic Maps
            <K, V> Map<K, V> createConcurrentWeakKeyedMap()
            <K, V> Map<K, V> createConcurrentTimeBasedEvictionMap(long ageInSeconds)
            <K, V> Map<K, V> createConcurrentAccessTimeBasedEvictionMap(long ageInSeconds, int initialCapacity)

            // Caches with Size/Capacity Constraints
            <K, V> Map<K, V> createCacheWithWeakKeysAndSize(int maxSize)
            <K, V> Map<K, V> createWeakKeyedCacheWithInitialCapacity(int initialCapacity)
            <K, V> Map<K, V> createCacheWithWeakKeysInitialCapacityAndSize(int initialCapacity, int maxSize)
            <K, V> Map<K, V> createCacheWithInitialCapacity(int initialCapacity)

            // Loading Caches (Function-based)
            <K, V> Function<K, V> memorize(Function<K, V> loader, int maxSize)
            <K, V> Function<K, V> createAccessTimeBasedCache(long ageInSeconds, int initialCapacity, Function<K, V> loader)
            <K, V> Function<K, V> createAccessTimeBasedCacheWithMaxSize(long ageInSeconds, int maxSize, Function<K, V> loader)
            <K, V> Function<K, V> createLoadingCache(Function<K, V> loader)
            <K, V> Function<K, V> createWeakKeyedLoadingCacheWithInitialCapacity(int initialCapacity, Function<K, V> loader)

            // Caches with Removal Listeners (CleanableMap Map wrapper)
            <K, V> CleanableMap<K, V> createCacheWithWriteExpirationAndRemovalListener(
                long age, TimeUnit unit, int initialCapacity, CacheRemovalListener<K, V> listener)
            <K, V> CleanableMap<K, V> createCacheWithAccessExpirationAndRemovalListener(
                long age, TimeUnit unit, int initialCapacity, CacheRemovalListener<K, V> listener)
                │
                │ returns
                ▼
          CleanableMap<K,V> (extends Map<K,V>)
            - cleanUp()
            - Standard Map methods          
```

### CollectionFactory Interface (`agent-bridge`)

Abstract factory interface that defines all cache creation methods. This interface is implemented by:
- `DefaultCollectionFactory` - No-op fallback using standard JDK collections (not used during normal agent operation)
- `AgentCollectionFactory` - Runtime factory that delegates to version-specific implementations

### CleanableMap Interface (`agent-bridge`)

Extends `Map<K, V>` to expose cache maintenance operations:
```java
public interface CleanableMap<K, V> extends Map<K, V> {
    void cleanUp();  // Triggers eviction of expired entries
}
```

### CacheRemovalListener Interface (`agent-bridge`)

Callback interface for cache removal events:
```java
public interface CacheRemovalListener<K, V> {
    enum RemovalReason {
        EXPIRED,    // Entry expired due to time-based policy
        EXPLICIT,   // Manually removed via remove() or clear()
        SIZE,       // Evicted due to size constraints
        REPLACED,   // Value replaced with put()
        COLLECTED   // Key or value was garbage collected
    }

    void onRemoval(K key, V value, RemovalReason reason);
}
```

### Caffeine2CollectionFactory (`newrelic-agent`)

Implementation using Caffeine 2.9.3 (Java 8-10):
- Located in `src/main/java/com/newrelic/agent/util/`
- Uses shaded package: `com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.*`

### Caffeine3CollectionFactory (`newrelic-agent`)

Implementation using Caffeine 3.2.3 (Java 11+):
- Located in `src/main/java11/com/newrelic/agent/util/`
- Uses shaded package: `com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine.cache.*`
- Identical API to Caffeine2CollectionFactory but uses newer Caffeine version

### AgentCollectionFactory (`newrelic-agent`)

Runtime delegation layer with two implementations:
- **Java 8 version** (`src/main/java/`) - Delegates to `Caffeine2CollectionFactory`
- **Java 11 version** (`src/main/java11/`) - Delegates to `Caffeine3CollectionFactory`

The JVM automatically selects the correct version based on the runtime Java version.

### Shading Tasks

Two Gradle tasks create shaded JARs with relocated Caffeine packages:

```bash
# Build both shaded JARs
gradle :newrelic-agent:shadeCaffeine2Jar :newrelic-agent:shadeCaffeine3Jar
```

### Package Relocation

Both versions are relocated to avoid classpath conflicts:

**Caffeine 2.9.3:**
```
com.github.benmanes.caffeine → com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine
```

**Caffeine 3.2.3:**
```
com.github.benmanes.caffeine → com.newrelic.agent.deps.caffeine3.com.github.benmanes.caffeine
```

The shaded dependencies are included in the main agent JAR:

```gradle
dependencies {
    // Shaded Caffeine 2 classes (Java 8-10)
    implementation files(shadeCaffeine2Jar.outputs.files)

    // Shaded Caffeine 3 classes (Java 11+) in META-INF/versions/11/
    java11Implementation files(shadeCaffeine3Jar.outputs.files)
}
```

## Refactored Classes

The com.newrelic.agent.PrivateApiImpl class bootstraps the `AgentBridge.collectionFactory` with `AgentCollectionFactory` during agent startup.
The correct `AgentCollectionFactory` is loaded by the VM based on the Java version runtime (via the multi-release jar).

The following classes now use `AgentBridge.collectionFactory` instead of direct Caffeine dependencies:

- com.newrelic.agent.TimedTokenSet - Token expiration tracking with access-based eviction and removal listener
- com.newrelic.agent.service.async.AsyncTransactionService - Async transaction registry with write-based expiration and removal listener
- com.newrelic.agent.ThreadService - Thread ID to name mapping with access-based eviction
- com.newrelic.agent.transaction.TransactionCache - Weak-keyed map for caching input streams
- com.newrelic.agent.database.CachingDatabaseStatementParser - Weak-keyed cache for parsed SQL statements (max size: 1000)
- com.newrelic.agent.sql.BoundedConcurrentCache - Generic bounded cache with initial capacity
- com.newrelic.agent.service.analytics.InsightsServiceImpl - String cache with access-based expiration (70s, max 1000)
- com.newrelic.agent.service.analytics.TransactionEventsService - Access-based cache with max samples stored (300s timeout)
- com.newrelic.agent.service.logging.LogSenderServiceImpl - String cache with access-based expiration (70s, max 1000)
- com.newrelic.agent.profile.v2.DiscoveryProfile - Profile trees cache with initial capacity
- com.newrelic.agent.profile.v2.Profile - Thread CPU times cache with initial capacity
- com.newrelic.agent.profile.v2.TransactionProfile - Thread profiles cache with initial capacity
- com.newrelic.agent.profile.v2.TransactionProfileSessionImpl - Transaction profile trees cache with initial capacity
- com.newrelic.agent.attributes.DefaultDestinationPredicate - Memoization cache for destination inclusion checks (max 200)
- com.newrelic.agent.tracers.metricname.MetricNameFormats - Metric name format cache with initial capacity
- com.newrelic.agent.cloud.AwsAccountDecoderImpl - Account ID decoder cache with access-based expiration (3600s)
- com.newrelic.agent.cloud.CloudAccountInfoCache - Weak-keyed cloud account info cache
- com.newrelic.agent.instrumentation.weaver.extension.ExtensionHolderFactoryImpl - Weak-keyed instance cache for extension holders
- com.newrelic.agent.threads.ThreadStateSampler - Thread tracker cache with access-based expiration (180s)

A special note about com.newrelic.weave.weavepackage.WeavePackageManager in the newrelic-weaver project:
- Added a dependency on agent-bridge to allow access to the AgentBridge.collectionFactory instance
- Added a test dependency on newrelic-agent to allow proper bootstrapping of the AgentCollectionFactory for tests

The following class doesn't use the CollectionFactory pattern:
- com.newrelic.bootstrap.EmbeddedJarFilesImpl - Uses `ConcurrentHashMap` directly due to chicken-and-egg problem: AgentBridge is 
not available until agent-bridge.jar is extracted and loaded
