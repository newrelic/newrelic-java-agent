package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class StripedCache<K,V>  {
    private final String cacheName;

    private final int[] hashBuckets;
    private final Map<Integer, Map<K, V>> cache;

    private final ScheduledExecutorService cacheReporter = Executors.newSingleThreadScheduledExecutor();

    public StripedCache(String cacheName) {
        this(cacheName, Integer.parseInt(System.getProperty("newrelic.config.num_stripes")));
    }

    public StripedCache(String cacheName, Supplier<Map<K,V>> cacheSupplier) {
        this(cacheName, Integer.parseInt(System.getProperty("newrelic.config.num_stripes")), cacheSupplier);
    }

    public StripedCache(String cacheName, int numStripes){
        this(cacheName, numStripes, AgentBridge.collectionFactory::createConcurrentWeakKeyedMap);
    }

    public StripedCache(String cacheName, int numStripes, Supplier<Map<K,V>> cacheSupplier) {
        this.cacheName = cacheName;
        this.hashBuckets = makeBuckets(numStripes);
        this.cache = buildCache(numStripes, cacheSupplier);
        cacheReporter.scheduleAtFixedRate(() -> System.out.println(this), 5L, 5L, TimeUnit.SECONDS);
    }

    public Map<K, V> getStripe(K key) {
        int stripe = getStripeForHash(key.hashCode());
        return cache.get(stripe);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cache ");
        sb.append(cacheName);
        sb.append("=");
        for (Map.Entry<Integer, Map<K, V>> entry : cache.entrySet()) {
            sb.append(entry.getKey());
            sb.append(":");
            sb.append(entry.getValue().size());
            sb.append(", ");
        }
        return sb.toString();
    }


    // ======= SETUP/INFRASTRUCTURE METHODS ============

    private int[] makeBuckets(int numStripes) {
        int positiveBucketCt = numStripes/2;
        int bucketSize = Integer.MAX_VALUE/positiveBucketCt;
        int[] result = new int[numStripes];
        for(int i = 0; i < numStripes; i++){
            result[i] = Integer.MIN_VALUE + (i * bucketSize);
        }
        return result;
    }

    private Map<Integer, Map<K, V>> buildCache(int numStripes, Supplier<Map<K,V>> cacheSupplier) {
        Map<Integer, Map<K, V>> map = new HashMap<>(numStripes);
        for (int i = 0; i <numStripes; i++) {
            map.put(i, cacheSupplier.get());
        }
        return map;
    }

    //returns a number from 0-9. This number will be used to map to the right cache for the given hashcode
    private int getStripeForHash(int hash){
        for (int i = hashBuckets.length - 1; i >= 0; i--){
            if (hash >= hashBuckets[i]){
                return i;
            }
        }
        return 0;
    }
}
