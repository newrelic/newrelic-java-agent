package org.redisson;

import java.util.Collection;

import org.redisson.api.RFuture;
import org.redisson.api.RList;
import org.redisson.api.RListMultimapCache;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonListMultimapCache<K, V> implements RListMultimapCache<K, V> {

	
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

    
    public RList<V> get(K key) {
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

}
