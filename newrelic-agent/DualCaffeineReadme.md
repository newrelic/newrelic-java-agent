# Dual Caffeine Version Support

## Overview

This document explains the implementation of dual Caffeine support in the agent. This change
was required because of the complete removal of the "Unsafe" class in Java 26. Caffeine v3+
eliminated any calls to the Unsafe class, however Caffeine v2 will not receive a similar
update. Caffeine v3 only supports Java 11+, so Caffeine v2 was still required to support
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
    - Shaded               - Shaded               - JDK Collections
      Caffeine 2.9.3         Caffeine 3.2.3         only
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
- `DefaultCollectionFactory` - No-op fallback using standard JDK collections(not used during normal agent operation)
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
# Build only the Caffeine 2.9.3 shaded JAR (Java 8-10)
gradle :newrelic-agent:shadeCaffeine2Jar

# Build only the Caffeine 3.2.3 shaded JAR (Java 11+)
gradle :newrelic-agent:shadeCaffeine3Jar

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

The following classes do **NOT** use the CollectionFactory pattern:

- com.newrelic.bootstrap.EmbeddedJarFilesImpl - Uses `ConcurrentHashMap` directly due to chicken-and-egg problem: AgentBridge is not available until agent-bridge.jar is extracted and loaded
- newrelic-weaver/WeavePackageManager - Uses built-in Java `WeakHashMap` and `ConcurrentHashMap` because the weaver module doesn't have access to agent-bridge

Both of these will result in zero performance impact since these caches will not grow after initial cache population.   

## Design Decisions

### Why Two Versions?

1. **Java 8 Compatibility**: Caffeine 2.9.3 is the last version supporting Java 8, which is still widely used
2. **Performance**: Caffeine 3.x uses modern Java features (VarHandle) for better performance on Java 11+
3. **Maintenance**: Using newer Caffeine versions ensures access to bug fixes and improvements

### Why Shading?

1. **Conflict Avoidance**: Prevents classpath conflicts if user applications use different Caffeine versions
2. **Version Control**: Agent can use specific, tested Caffeine versions regardless of user dependencies
3. **Isolation**: Changes to Caffeine don't affect agent's internal caching behavior

### Why Factory Pattern?

1. **Abstraction**: Agent code doesn't depend on specific Caffeine versions
2. **Testability**: Tests can use real implementations or mocks as needed
3. **Flexibility**: Easy to add new cache types or switch implementations
4. **Bridge Module**: Factory interface lives in `agent-bridge` which has no dependencies

## Troubleshooting

### NoClassDefFoundError for Shaded Caffeine Classes

**Problem**: Tests fail with `NoClassDefFoundError: com/newrelic/agent/deps/caffeine2/...`

**Solution**: Ensure shaded JARs are on test runtime classpath in `build.gradle`:
```gradle
testRuntimeOnly files(shadeCaffeine2Jar.outputs.files)
testRuntimeOnly files(shadeCaffeine3Jar.outputs.files)
```

### Cache Not Expiring in Tests

**Problem**: Time-based eviction doesn't work, cache size doesn't decrease

**Solution**: Initialize `AgentBridge.collectionFactory` in test setup:
```java
AgentBridge.collectionFactory = new AgentCollectionFactory();
```

Without this initialization, tests use `DefaultCollectionFactory` which provides no-op implementations.

### Removal Listener Not Called

**Problem**: `CacheRemovalListener.onRemoval()` never executes

**Solution**:
1. Call `cleanUp()` explicitly to trigger expired entry removal:
   ```java
   ((CleanableMap<K, V>) cache).cleanUp();
   ```
2. Caffeine's removal listeners are asynchronous - use `executor(Runnable::run)` for synchronous testing

## References

- **Caffeine Cache**: https://github.com/ben-manes/caffeine
- **Multi-Release JARs**: https://openjdk.org/jeps/238
- **Gradle Shadow Plugin**: https://github.com/johnrengelman/shadow (used for shading)

## Files Modified

### Core Implementation
- `agent-bridge/src/main/java/com/newrelic/agent/bridge/CollectionFactory.java`
- `agent-bridge/src/main/java/com/newrelic/agent/bridge/CleanableMap.java`
- `agent-bridge/src/main/java/com/newrelic/agent/bridge/CacheRemovalListener.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/util/Caffeine2CollectionFactory.java`
- `newrelic-agent/src/main/java11/com/newrelic/agent/util/Caffeine3CollectionFactory.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/util/AgentCollectionFactory.java`
- `newrelic-agent/src/main/java11/com/newrelic/agent/util/AgentCollectionFactory.java`

### Refactored Classes (All using CollectionFactory)

**Core Agent:**
- `newrelic-agent/src/main/java/com/newrelic/agent/TimedTokenSet.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/ThreadService.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/PrivateApiImpl.java` (initialization)
- `newrelic-agent/src/main/java/com/newrelic/agent/transaction/TransactionCache.java`

**Services:**
- `newrelic-agent/src/main/java/com/newrelic/agent/service/async/AsyncTransactionService.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/service/analytics/InsightsServiceImpl.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/service/analytics/TransactionEventsService.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/service/logging/LogSenderServiceImpl.java`

**Database & SQL:**
- `newrelic-agent/src/main/java/com/newrelic/agent/database/CachingDatabaseStatementParser.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/sql/BoundedConcurrentCache.java`

**Profiling:**
- `newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/DiscoveryProfile.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/Profile.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/TransactionProfile.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/profile/v2/TransactionProfileSessionImpl.java`

**Attributes & Metrics:**
- `newrelic-agent/src/main/java/com/newrelic/agent/attributes/DefaultDestinationPredicate.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/tracers/metricname/MetricNameFormats.java`

**Cloud & Infrastructure:**
- `newrelic-agent/src/main/java/com/newrelic/agent/cloud/AwsAccountDecoderImpl.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/cloud/CloudAccountInfoCache.java`

**Instrumentation:**
- `newrelic-agent/src/main/java/com/newrelic/agent/instrumentation/weaver/extension/ExtensionHolderFactoryImpl.java`
- `newrelic-agent/src/main/java/com/newrelic/agent/threads/ThreadStateSampler.java`

### Test Infrastructure
- `newrelic-agent/src/test/java/com/newrelic/agent/TransactionAsyncUtility.java`
- `newrelic-agent/src/test/java/com/newrelic/agent/tracing/BaseDistributedTraceTest.java`
- `newrelic-agent/src/test/java/com/newrelic/agent/transaction/AbstractPriorityTransactionNamingPolicyTest.java`
- `newrelic-agent/src/test/java/com/newrelic/agent/TransactionTest.java`

### Build Configuration
- `newrelic-agent/build.gradle` - Shading tasks, dependency configuration, Multi-Release JAR setup
