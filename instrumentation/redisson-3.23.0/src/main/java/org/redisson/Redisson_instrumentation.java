package org.redisson;

import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.MapCacheOptions;
import org.redisson.api.MapOptions;
import org.redisson.api.RAtomicDouble;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBinaryStream;
import org.redisson.api.RBitSet;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBoundedBlockingQueue;
import org.redisson.api.RBucket;
import org.redisson.api.RBuckets;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RDeque;
import org.redisson.api.RGeo;
import org.redisson.api.RHyperLogLog;
import org.redisson.api.RLexSortedSet;
import org.redisson.api.RList;
import org.redisson.api.RListMultimap;
import org.redisson.api.RListMultimapCache;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RPatternTopic;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RPriorityDeque;
import org.redisson.api.RPriorityQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSemaphore;
import org.redisson.api.RSet;
import org.redisson.api.RSetCache;
import org.redisson.api.RSetMultimap;
import org.redisson.api.RSetMultimapCache;
import org.redisson.api.RSortedSet;
import org.redisson.api.RTopic;
import org.redisson.client.codec.Codec;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName="org.redisson.Redisson")
public abstract class Redisson_instrumentation {

    
    public RBinaryStream getBinaryStream(String name) {
    	RBinaryStream obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BinaryStream";
    	}
        return obj;
    }
    
    
    public <V> RGeo<V> getGeo(String name) {
    	RGeo<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Geo";
    	}
        return obj;
    }
    
    
    public <V> RGeo<V> getGeo(String name, Codec codec) {
    	RGeo<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Geo";
    	}
        return obj;
    }

    
    public <V> RBucket<V> getBucket(String name) {
    	RBucket<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Bucket";
    	}
        return obj;
    }

    
    public <V> RBucket<V> getBucket(String name, Codec codec) {
    	RBucket<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Bucket";
    	}
        return obj;
    }

    
    public RBuckets getBuckets() {
    	RBuckets obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Buckets";
    	}
        return obj;
    }
    
    
    public RBuckets getBuckets(Codec codec) {
    	RBuckets obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Buckets";
    	}
        return obj;
    }
    
    
    public <V> RHyperLogLog<V> getHyperLogLog(String name) {
    	RHyperLogLog<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "HyperLogLog";
    	}
        return obj;
    }

    
    public <V> RHyperLogLog<V> getHyperLogLog(String name, Codec codec) {
    	RHyperLogLog<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "HyperLogLog";
    	}
        return obj;
    }

    
    public <V> RList<V> getList(String name) {
    	RList<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "List";
    	}
        return obj;
    }

    
    public <V> RList<V> getList(String name, Codec codec) {
    	RList<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "List";
    	}
        return obj;
    }

    
    public <K, V> RListMultimap<K, V> getListMultimap(String name) {
    	RListMultimap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "ListMultiMap";
    	}
        return obj;
    }

    
    public <K, V> RListMultimap<K, V> getListMultimap(String name, Codec codec) {
    	RListMultimap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "ListMultiMap";
    	}
        return obj;
    }

    
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name, LocalCachedMapOptions<K, V> options) {
    	RLocalCachedMap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "LocalCachedMap";
    	}
        return obj;
    }

    
    public <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name, Codec codec, LocalCachedMapOptions<K, V> options) {
    	RLocalCachedMap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "LocalCachedMap";
    	}
        return obj;
    }

    
    public <K, V> RMap<K, V> getMap(String name) {
    	RMap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Map";
    	}
        return obj;
    }
    
    
    public <K, V> RMap<K, V> getMap(String name, MapOptions<K, V> options) {
    	RMap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Map";
    	}
        return obj;
    }

    
    public <K, V> RSetMultimap<K, V> getSetMultimap(String name) {
    	RSetMultimap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "SetMultiMap";
    	}
        return obj;
    }
    
    
    public <K, V> RSetMultimapCache<K, V> getSetMultimapCache(String name) {
    	RSetMultimapCache<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "SetMultiMapCache";
    	}
        return obj;
    }
    
    
    public <K, V> RSetMultimapCache<K, V> getSetMultimapCache(String name, Codec codec) {
    	RSetMultimapCache<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "SetMultiMapCache";
    	}
        return obj;
    }

    
    public <K, V> RListMultimapCache<K, V> getListMultimapCache(String name) {
    	RListMultimapCache<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "ListMultiMapCache";
    	}
        return obj;
    }
    
    
    public <K, V> RListMultimapCache<K, V> getListMultimapCache(String name, Codec codec) {
    	RListMultimapCache<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "ListMultiMapCache";
    	}
        return obj;
    }

    
    public <K, V> RSetMultimap<K, V> getSetMultimap(String name, Codec codec) {
    	RSetMultimap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "SetMultiMap";
    	}
        return obj;
    }

    
    public <V> RSetCache<V> getSetCache(String name) {
    	RSetCache<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "SetCache";
    	}
        return obj;
    }

    
    public <V> RSetCache<V> getSetCache(String name, Codec codec) {
    	RSetCache<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "SetCache";
    	}
        return obj;
    }

    
    public <K, V> RMapCache<K, V> getMapCache(String name) {
    	RMapCache<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "MapCache";
    	}
        return obj;
    }

    
    public <K, V> RMapCache<K, V> getMapCache(String name, MapCacheOptions<K, V> options) {
    	RMapCache<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "MapCache";
    	}
        return obj;
    }
    
    
    public <K, V> RMapCache<K, V> getMapCache(String name, Codec codec) {
    	RMapCache<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "MapCache";
    	}
        return obj;
    }
    
    
    public <K, V> RMapCache<K, V> getMapCache(String name, Codec codec, MapCacheOptions<K, V> options) {
    	RMapCache<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "MapCache";
    	}
        return obj;
    }

    
    public <K, V> RMap<K, V> getMap(String name, Codec codec) {
    	RMap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Map";
    	}
        return obj;
    }
    
    
    public <K, V> RMap<K, V> getMap(String name, Codec codec, MapOptions<K, V> options) {
    	RMap<K,V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Map";
    	}
        return obj;
    }

    
    public RLock getLock(String name) {
    	RLock obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Lock";
    	}
        return obj;
    }

    
    public RLock getFairLock(String name) {
    	RLock obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "FairLock";
    	}
        return obj;
    }
    
    
    public RReadWriteLock getReadWriteLock(String name) {
    	RReadWriteLock obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "ReadWriteLock";
    	}
        return obj;
    }

    
    public <V> RSet<V> getSet(String name) {
    	RSet<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Set";
    	}
        return obj;
    }

    
    public <V> RSet<V> getSet(String name, Codec codec) {
    	RSet<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Set";
    	}
        return obj;
    }

    
    public <V> RSortedSet<V> getSortedSet(String name) {
    	RSortedSet<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "SortedSet";
    	}
        return obj;
    }

    
    public <V> RSortedSet<V> getSortedSet(String name, Codec codec) {
    	RSortedSet<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "SortedSet";
    	}
        return obj;
    }

    
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name) {
    	RScoredSortedSet<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "ScoredSortedSet";
    	}
        return obj;
    }

    
    public <V> RScoredSortedSet<V> getScoredSortedSet(String name, Codec codec) {
    	RScoredSortedSet<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "ScoredSortedSet";
    	}
        return obj;
    }

    
    public RLexSortedSet getLexSortedSet(String name) {
    	RLexSortedSet obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "LexSortedSet";
    	}
        return obj;
    }

    
    public <M> RTopic getTopic(String name) {
    	RTopic obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Topic";
    	}
        return obj;
    }

    
    public <M> RTopic getTopic(String name, Codec codec) {
    	RTopic obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Topic";
    	}
        return obj;
    }

    
    public <M> RPatternTopic getPatternTopic(String pattern) {
    	RPatternTopic obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "PatternTopic";
    	}
        return obj;
    }

    
    public <M> RPatternTopic getPatternTopic(String pattern, Codec codec) {
    	RPatternTopic obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "PatternTopic";
    	}
        return obj;
    }

    
    public <V> RDelayedQueue<V> getDelayedQueue(RQueue<V> destinationQueue) {
    	RDelayedQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "DelayedQueue";
    	}
        return obj;
    }
    
    
    public <V> RQueue<V> getQueue(String name) {
    	RQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Queue";
    	}
        return obj;
    }

    
    public <V> RQueue<V> getQueue(String name, Codec codec) {
    	RQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Queue";
    	}
        return obj;
    }

    
    public <V> RBlockingQueue<V> getBlockingQueue(String name) {
    	RBlockingQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BlockingQueue";
    	}
        return obj;
    }

    
    public <V> RBlockingQueue<V> getBlockingQueue(String name, Codec codec) {
    	RBlockingQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BlockingQueue";
    	}
        return obj;
    }
    
    
    public <V> RBoundedBlockingQueue<V> getBoundedBlockingQueue(String name) {
    	RBoundedBlockingQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BoundedBlockingQueue";
    	}
        return obj;
    }

    
    public <V> RBoundedBlockingQueue<V> getBoundedBlockingQueue(String name, Codec codec) {
    	RBoundedBlockingQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BoundedBlockingQueue";
    	}
        return obj;
    }

    
    public <V> RDeque<V> getDeque(String name) {
    	RDeque<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Deque";
    	}
        return obj;
    }

    
    public <V> RDeque<V> getDeque(String name, Codec codec) {
    	RDeque<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Deque";
    	}
        return obj;
    }

    
    public <V> RBlockingDeque<V> getBlockingDeque(String name) {
    	RBlockingDeque<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BlockingDeque";
    	}
        return obj;
    }

    
    public <V> RBlockingDeque<V> getBlockingDeque(String name, Codec codec) {
    	RBlockingDeque<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BlockingDeque";
    	}
        return obj;
    };

    
    public RAtomicLong getAtomicLong(String name) {
    	RAtomicLong obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "AtomicLong";
    	}
        return obj;
    }

    
    public RAtomicDouble getAtomicDouble(String name) {
    	RAtomicDouble obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "AtomicDouble";
    	}
        return obj;
    }

    
    public RBitSet getBitSet(String name) {
    	RBitSet obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BitSet";
    	}
        return obj;
    }

    
    public RSemaphore getSemaphore(String name) {
    	RSemaphore obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "Semaphore";
    	}
        return obj;
    }
    
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String name) {
    	RPermitExpirableSemaphore obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "PermitExpirableSemaphore";
    	}
        return obj;
    }


    
    public <V> RBloomFilter<V> getBloomFilter(String name) {
    	RBloomFilter<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BloomFilter";
    	}
        return obj;
    }

    
    public <V> RBloomFilter<V> getBloomFilter(String name, Codec codec) {
    	RBloomFilter<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "BloomFilter";
    	}
        return obj;
    }

    
    public <V> RPriorityQueue<V> getPriorityQueue(String name) {
    	RPriorityQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "PriorityQueue";
    	}
        return obj;
    }

    
    public <V> RPriorityQueue<V> getPriorityQueue(String name, Codec codec) {
    	RPriorityQueue<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "PriorityQueue";
    	}
        return obj;
    }
    
    
    public <V> RPriorityDeque<V> getPriorityDeque(String name) {
    	RPriorityDeque<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "PriorityDeque";
    	}
        return obj;
    }

    
    public <V> RPriorityDeque<V> getPriorityDeque(String name, Codec codec) {
    	RPriorityDeque<V> obj = Weaver.callOriginal();
    	if(obj instanceof RedissonObject) {
    		RedissonObject redissonObj = (RedissonObject)obj;
    		redissonObj.objectName = "PriorityDeque";
    	}
        return obj;
    }
    
}
