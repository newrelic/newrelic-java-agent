package org.redisson.reactive;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;
import org.redisson.api.RBlockingQueueReactive;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonBlockingQueueReactive<V> implements RBlockingQueueReactive<V> {

    public Publisher<Integer> put(V e) {
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

    public Publisher<V> take() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TAKE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<V> poll(long timeout, TimeUnit unit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<V> pollFromAny(long timeout, TimeUnit unit, String ... queueNames) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLFROMANY");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<V> pollLastAndOfferFirstTo(String queueName, long timeout, TimeUnit unit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLLASTANDOFFERFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> drainTo(Collection<? super V> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DRAINTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> drainTo(Collection<? super V> c, int maxElements) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DRAINTO");
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
