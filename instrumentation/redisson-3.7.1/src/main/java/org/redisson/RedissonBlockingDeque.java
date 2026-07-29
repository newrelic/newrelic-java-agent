package org.redisson;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RBlockingDeque;
import org.redisson.api.RFuture;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonBlockingDeque<V> implements RBlockingDeque<V>{



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

	public V take() throws InterruptedException {
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

	public V poll(long timeout, TimeUnit unit) throws InterruptedException {
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

	public V pollFromAny(long timeout, TimeUnit unit, String ... queueNames) throws InterruptedException {
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
			Utils.setOperation("POLLLASTANDOFFERFIRSTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public V pollLastAndOfferFirstTo(String queueName, long timeout, TimeUnit unit) throws InterruptedException {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLLASTANDOFFERFIRSTTO");
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
			Utils.setOperation("TAKELASTANDOFFERFIRSTTO");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public int drainTo(Collection<? super V> c) {
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


	public RFuture<Integer> drainToAsync(Collection<? super V> c) {
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


	public int drainTo(Collection<? super V> c, int maxElements) {
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


	public RFuture<Integer> drainToAsync(Collection<? super V> c, int maxElements) {
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


	public RFuture<Void> putFirstAsync(V e) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUTFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public RFuture<Void> putLastAsync(V e) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUTLAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public void putFirst(V e) throws InterruptedException {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUTFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
	}


	public void putLast(V e) throws InterruptedException {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUTLAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
	}


	public boolean offerFirst(V e, long timeout, TimeUnit unit) throws InterruptedException {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("OFFERFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public boolean offerLast(V e, long timeout, TimeUnit unit) throws InterruptedException {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("OFFERLAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public RFuture<V> takeFirstAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TAKEFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public RFuture<V> takeLastAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TAKELAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public RFuture<V> pollFirstAsync(long timeout, TimeUnit unit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public V pollFirstFromAny(long timeout, TimeUnit unit, String ... queueNames) throws InterruptedException {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLFIRSTFROMANY");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public RFuture<V> pollFirstFromAnyAsync(long timeout, TimeUnit unit, String ... queueNames) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLFIRSTFROMANY");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}


	public RFuture<V> pollLastFromAnyAsync(long timeout, TimeUnit unit, String ... queueNames) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLLASTFROMANY");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<V> pollLastAsync(long timeout, TimeUnit unit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POLLLAST");
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
