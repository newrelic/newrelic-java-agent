package org.redisson.reactive;

import java.util.Collection;

import org.reactivestreams.Publisher;
import org.redisson.api.RHyperLogLogReactive;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave
public abstract class RedissonHyperLogLogReactive<V> implements RHyperLogLogReactive<V> {

    public Publisher<Boolean> add(V obj) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> addAll(Collection<V> objects) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Long> count() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("COUNT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Long> countWith(String... otherLogNames) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("COUNTWITH");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> mergeWith(String... otherLogNames) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("MERGEWITH");
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
