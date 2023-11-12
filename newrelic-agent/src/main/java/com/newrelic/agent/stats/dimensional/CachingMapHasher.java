package com.newrelic.agent.stats.dimensional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This map hasher maintains a map of map instance id to hash.  The map cache is updated by
 * calling {@link #addHash(Map, long)}.
 */
class CachingMapHasher implements MapHasher {
    private final MapHasher mapHasher;
    private final Map<Integer, Long> cachedHashesByIdentity = new ConcurrentHashMap<>();

    public CachingMapHasher(MapHasher mapHasher) {
        this.mapHasher = mapHasher;
    }

    @Override
    public long hash(Map<String, ?> map) {
        final int identityHashCode = System.identityHashCode(map);
        final Long hash = cachedHashesByIdentity.get(identityHashCode);
        return hash == null ? mapHasher.hash(map) : hash;
    }

    public void addHash(Map<String, ?> map, long hash) {
        // empty maps are already special cased, we don't need to add them
        if (!map.isEmpty()) {
            cachedHashesByIdentity.put(System.identityHashCode(map), hash);
        }
    }

    public void reset() {
        cachedHashesByIdentity.clear();
    }
}
