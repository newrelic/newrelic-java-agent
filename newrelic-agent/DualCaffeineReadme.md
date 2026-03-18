# Dual Caffeine Version Support

## Overview

This document explains the implementation of dual Caffeine support in the agent. This change
was required because of the complete removal of the "Unsafe" class in Java 26. Caffeine v3+
eliminated any calls to the Unsafe class, however Caffeine v2 will not receive a similar
update. Caffeine v3 only supports Java 11+, so Caffeine v2 is still required to support
Java 8 - 10.

- **Caffeine 2.9.3** - For Java 8-10 (uses `sun.misc.Unsafe`)
- **Caffeine 3.2.3** - For Java 11+ (uses `VarHandle`)

Both versions are shaded with relocated package names to avoid conflicts with user applications,
and the agent automatically selects the appropriate version at runtime based on the Java version,
utilizing the CollectionFactory on the AgentBridge.

A config exists, that if set to `true`, will force the use of Caffeine v2, even if the Java version is 11+:
- System property: `newrelic.config.collectionfactory.forcev2`
- Environment variable: `NEW_RELIC_COLLECTIONFACTORY_FORCEV2`

(Not available as a yaml config since the AgentCollectionFactory is loaded prior to any services being
spun up).

A couple of other Caffeine v2 and v3 differences:
- v2 depends on `org.checkerframework`, which is also relocated in the agent jar
- v3 depends on `org.jspecify`, which is also relocated in the agent jar
- v3 changed from `TimeUnit` to `java.time.Duration`. The Caffeine3CollectionFactory handles this automatically.

A couple of somewhat gross implementation details:
- Both versions of Caffeine are shaded into the Agent jar
- The shaded package name for Caffeine is used in the `import` statements in the CollectionFactory
- A roll-our-own weak-key LRU cache in the weaver project, since this project doesn't (and should not)
  have a dependency on the agent bridge
- A custom implementation of a CleanableMap to be used with removal listener caches

#### Why not transition to a new cache provider??
- cache2k, ehcache - No support for weak keyed caches
- guava - suffers from the same `Unsafe` usage that Caffeine v2 does
- "Roll your own" caches in vanilla Java - Extremely difficult and brittle to implement weak key caches
  and max size caches with acceptable performance (although one was created for the weaver project)

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

##### A Note on CaffeineXCollectionFactory Tests

The code includes a comprehensive test suite for Caffeine2CollectionFactory but not for Caffeine3CollectionFactory
(intentionally). Both implementations provide identical behavior from the consumer's perspective, differing only in
the underlying Caffeine version.

### AgentCollectionFactory (`newrelic-agent`)

Runtime delegation layer that uses reflection to dynamically load the appropriate Caffeine factory:
- **For Java 8-10** - Delegates to `Caffeine2CollectionFactory`
- **For Java 11+** - Delegates to `Caffeine3CollectionFactory`

### Caffeine Adapters and Bridge Interfaces

To maintain a clean separation between the agent-bridge API and the underlying Caffeine implementations, several
adapter patterns and bridge interfaces were added.

## CleanableMap Interface (agent-bridge)

The CleanableMap interface extends Map<K, V> to expose cache maintenance operations that may be deferred by the underlying cache implementation:

```java
public interface CleanableMap<K, V> extends Map<K, V> {                                                                                                                                                                                            
    /**                                                                                                                                                                                                                                            
     * Perform any pending maintenance operations, such as:                                                                                                                                                                                        
     * - Evicting expired entries                                                                                                                                                                                                                  
     * - Invoking removal listeners for pending removals                                                                                                                                                                                           
     * - Performing size-based evictions                                                                                                                                                                                                           
     */                                                                                                                                                                                                                                            
    void cleanUp();                                                                                                                                                                                                                                
}
```

Purpose: Caffeine defers maintenance operations for performance reasons. The cleanUp() method provides explicit control over when maintenance
occurs

## CacheRemovalListener Interface (agent-bridge)

Provides a unified callback interface for cache removal events, decoupled from the specific Caffeine version:

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

Purpose: This interface prevents the agent code from direct dependencies on Caffeine's RemovalCause enum.

## CaffeineCleanableMap Adapter

Both Caffeine2CollectionFactory and Caffeine3CollectionFactory contain an inner CaffeineCleanableMap class that
adapts Caffeine's Cache interface to the agent-bridge's CleanableMap interface:

```java
private static class CaffeineCleanableMap<K, V> implements CleanableMap<K, V> {                                                                                                                                                                    
    private final Cache<K, V> cache;                                                                                                                                                                                                               
    private final Map<K, V> map;

      CaffeineCleanableMap(Cache<K, V> cache) {                                                                                                                                                                                                      
          this.cache = cache;                                                                                                                                                                                                                        
          this.map = cache.asMap();                                                                                                                                                                               
      }                                                                                                                                                                                                                                              
                                                                                                                                                                                                                                                     
      @Override                                                                                                                                                                                                                                      
      public void cleanUp() {
          // Delegate to Caffeine's cleanUp() method
          cache.cleanUp();                                                                                                                                                                                      
      }                                                                                                                                                                                                                                              
                                                                                                                                                                                                                                                     
      // All Map methods delegate to cache.asMap()                                                                                                                                                                                                   
      @Override public int size() { return map.size(); }                                                                                                                                                                                             

      // ...remaining Map methods                                                                                                                                                                                                                 
}
```

- Maintains both a Cache reference (for `cleanUp()`) and a Map reference (for standard Map operations)
- All Map operations delegate to cache.asMap(), which is a standard `java.util.concurrent.ConcurrentMap` view
- The `cleanUp()` method proxies to Caffeine's equivalent cleanup method

## RemovalCause Conversion

Each factory class contains a convertRemovalCause() method that maps Caffeine's version-specific RemovalCause
enum to the agent-bridge's RemovalReason enum:

Caffeine2CollectionFactory:
``java
    private CacheRemovalListener.RemovalReason convertRemovalCause(                                                                                                                                                                                    
    com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.RemovalCause cause) {                                                                                                                                                 
        switch (cause) {                                                                                                                                                                                                                               
        case EXPIRED:   return CacheRemovalListener.RemovalReason.EXPIRED;                                                                                                                                                                         
        case SIZE:      return CacheRemovalListener.RemovalReason.SIZE;                                                                                                                                                                            
        case REPLACED:  return CacheRemovalListener.RemovalReason.REPLACED;                                                                                                                                                                        
        case COLLECTED: return CacheRemovalListener.RemovalReason.COLLECTED;                                                                                                                                                                       
        default:        return CacheRemovalListener.RemovalReason.EXPLICIT;                                                                                                                                                                        
    }                                                                                                                                                                                                                                              
}
``

The Caffeine3CollectionFactory has identical logic but takes a v3 `RemovalCause` instance.

### Shading Tasks

Two Gradle tasks create shaded JARs with relocated Caffeine packages (both are included in the final agent jar):

```bash
# Build both shaded JARs
gradle :newrelic-agent:shadeCaffeine2Jar :newrelic-agent:shadeCaffeine3Jar
```

If a full agent build is performed, running the new `shadeCaffeineXJar` tasks is unnecessary.

**Note:** This task (or a full agent build) will need to be run to make IntelliJ happy and eliminate
errors and warnings in the project.

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

The following classes now use `AgentBridge.collectionFactory` instead of direct Caffeine dependencies:

- `com.newrelic.agent.TimedTokenSet` - Token expiration tracking with access-based eviction and removal listener
- `com.newrelic.agent.service.async.AsyncTransactionService` - Async transaction registry with write-based expiration and removal listener
- `com.newrelic.agent.ThreadService` - Thread ID to name mapping with access-based eviction
- `com.newrelic.agent.transaction.TransactionCache` - Weak-keyed map for caching input streams
- `com.newrelic.agent.database.CachingDatabaseStatementParser` - Weak-keyed cache for parsed SQL statements (max size: 1000)
- `com.newrelic.agent.sql.BoundedConcurrentCache` - Generic bounded cache with initial capacity
- `com.newrelic.agent.service.analytics.InsightsServiceImpl` - String cache with access-based expiration (70s, max 1000)
- `com.newrelic.agent.service.analytics.TransactionEventsService` - Access-based cache with max samples stored (300s timeout)
- `com.newrelic.agent.service.logging.LogSenderServiceImpl` - String cache with access-based expiration (70s, max 1000)
- `com.newrelic.agent.profile.v2.DiscoveryProfile` - Profile trees cache with initial capacity
- `com.newrelic.agent.profile.v2.Profile` - Thread CPU times cache with initial capacity
- `com.newrelic.agent.profile.v2.TransactionProfile` - Thread profiles cache with initial capacity
- `com.newrelic.agent.profile.v2.TransactionProfileSessionImpl` - Transaction profile trees cache with initial capacity
- `com.newrelic.agent.attributes.DefaultDestinationPredicate` - Memoization cache for destination inclusion checks (max 200)
- `com.newrelic.agent.tracers.metricname.MetricNameFormats` - Metric name format cache with initial capacity
- `com.newrelic.agent.cloud.AwsAccountDecoderImpl` - Account ID decoder cache with access-based expiration (3600s)
- `com.newrelic.agent.cloud.CloudAccountInfoCache` - Weak-keyed cloud account info cache
- `com.newrelic.agent.instrumentation.weaver.extension.ExtensionHolderFactoryImpl` - Weak-keyed instance cache for extension holders
- `com.newrelic.agent.threads.ThreadStateSampler` - Thread tracker cache with access-based expiration (180s)

**A special note about com.newrelic.weave.weavepackage.WeavePackageManager in the newrelic-weaver project:**

This class doesn't use the wrapped caffeine library for its weak-keyed cache. Rather it uses a custom-built
weak key LRU cache (`WeakKeyLruCache`) using standard Java classes (`WeakHashMap` and `LinkedList`). This was
done because the weaver doesn't have a dependency on the agent-bridge project, and adding this dependency would
be fairly messy. The custom implementation provides reasonable performance (tested with 10,000 operations), and
this cache isn't in a hot code path after initial weaving takes place.

The following class doesn't use the CollectionFactory pattern or custom `WeakeKeyLruCache` implementation:
- `com.newrelic.bootstrap.EmbeddedJarFilesImpl` - Uses `ConcurrentHashMap` directly due to chicken-and-egg problem: AgentBridge is
  not available until agent-bridge.jar is extracted and loaded by the EmbeddedJarFilesImpl service.
