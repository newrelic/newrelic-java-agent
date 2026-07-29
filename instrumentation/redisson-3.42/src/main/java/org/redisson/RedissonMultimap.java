package org.redisson;

import java.util.List;
import java.util.Set;

import org.redisson.api.RFuture;
import org.redisson.api.RMultimap;
import org.redisson.client.protocol.RedisCommand;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonMultimap<K, V> implements RMultimap<K, V> {
	
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

    protected <T> RFuture<T> fastRemoveAsync(List<Object> mapKeys, List<Object> listKeys, RedisCommand<T> evalCommandType) {
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
    
    public RFuture<Boolean> deleteAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DELETE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<Integer> keySizeAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("KEYSIZE");
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
