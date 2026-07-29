package org.redisson;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.redisson.api.AsyncIterator;
import org.redisson.api.RFuture;
import org.redisson.api.RMap;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonMap<K, V> implements RMap<K, V> {

	
    public RFuture<Integer> sizeAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SIZE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Integer> valueSizeAsync(K key) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("VALUESIZE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> containsKeyAsync(Object key) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CONTAINSKEY");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<Boolean> containsValueAsync(Object value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CONTAINSVALUE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Map<K, V>> getAllAsync(Set<K> keys) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Void> putAllAsync(Map<? extends K, ? extends V> map) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUTALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public void clear() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CLEAR");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }

    public RFuture<Set<K>> readAllKeySetAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READALLKEYSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Collection<V>> readAllValuesAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READALLVALUES");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Set<Entry<K, V>>> readAllEntrySetAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READALLENTRYSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Map<K, V>> readAllMapAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("READALLMAP");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<V> putIfAbsentAsync(K key, V value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUTIFABSENT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> fastPutIfAbsentAsync(K key, V value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FASTPUTIFABSENT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> removeAsync(Object key, Object value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> replaceAsync(K key, V oldValue, V newValue) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REPLACE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<V> replaceAsync(K key, V value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REPLACE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<V> getAsync(K key) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    @SuppressWarnings("unused")
	private RFuture<Void> loadAllAsync(Supplier<Spliterator<K>> supplier, boolean replaceExistingValues, int parallelism) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("LOADALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();

    }
    
    CompletionStage<List<K>> loadAllAsync(AsyncIterator<K> iterator, List<K> elements, AtomicInteger workers) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("LOADALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<V> putAsync(K key, V value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<V> removeAsync(K key) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> fastPutAsync(K key, V value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FASTPUT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    protected RFuture<Boolean> fastPutOperationAsync(K key, V value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FASTPUT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    @SuppressWarnings("unchecked")
	public RFuture<Long> fastRemoveAsync(K ... keys) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FASTREMOVE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<V> addAndGetAsync(K key, Number value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ANDANDGET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

}
