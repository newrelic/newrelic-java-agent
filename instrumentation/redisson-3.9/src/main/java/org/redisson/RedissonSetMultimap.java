package org.redisson;

import java.util.Collection;

import org.redisson.api.RFuture;
import org.redisson.api.RSetMultimap;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonSetMultimap<K, V> implements RSetMultimap<K, V> {

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

    public RFuture<Boolean> containsKeyAsync(Object key) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CONTAINS");
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

    public RFuture<Boolean> containsEntryAsync(Object key, Object value) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CONTAINSENTRY");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> putAsync(K key, V value) {
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

    public RFuture<Boolean> putAllAsync(K key, Iterable<? extends V> values) {
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

    public RFuture<Collection<V>> getAllAsync(K key) {
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

    public RFuture<Collection<V>> removeAllAsync(Object key) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVEALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Collection<V>> replaceValuesAsync(K key, Iterable<? extends V> values) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REPLACEVALUES");
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
