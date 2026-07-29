package org.redisson;

import org.redisson.api.RAtomicDouble;
import org.redisson.api.RFuture;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonAtomicDouble implements RAtomicDouble {


	public RFuture<Double> addAndGetAsync(double delta) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDANDGET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Boolean> compareAndSetAsync(double expect, double update) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDANDGET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Double> decrementAndGetAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DECREMENTANDGET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public double get() {
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

	public RFuture<Double> getAndAddAsync(final double delta) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETANDADD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public double getAndDecrement() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETANDDECREMENT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Double> getAndDecrementAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETANDDECREMENT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Double> getAndSetAsync(double newValue) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETANDSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Double> incrementAndGetAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("INCREMENTANDGET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public double getAndIncrement() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETANDINCREMENT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Double> getAndIncrementAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETANDINCREMENT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Void> setAsync(double newValue) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SET");
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
