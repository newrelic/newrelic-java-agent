package org.redisson;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RBlockingQueue;
import org.redisson.api.RFuture;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonBlockingQueue<V> implements RBlockingQueue<V> {



	public RFuture<Void> putAsync(V e) {
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

	public RFuture<V> takeAsync() {
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

	public RFuture<V> pollAsync(long timeout, TimeUnit unit) {
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

	public RFuture<V> pollFromAnyAsync(long timeout, TimeUnit unit, String ... queueNames) {
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


	public RFuture<V> pollLastAndOfferFirstToAsync(String queueName, long timeout, TimeUnit unit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("pollLastAndOfferFirstTo".toUpperCase());
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public RFuture<V> takeLastAndOfferFirstToAsync(String queueName) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("takeLastAndOfferFirstTo".toUpperCase());
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Integer> drainToAsync(Collection<? super V> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DrainTo".toUpperCase());
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}



	public RFuture<Integer> drainToAsync(Collection<? super V> c, int maxElements) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DrainTo".toUpperCase());
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
