package org.redisson;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RFuture;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonAtomicLong implements RAtomicLong {


	public RFuture<Long> addAndGetAsync(long delta) {
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

	public RFuture<Boolean> compareAndSetAsync(long expect, long update) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("COMPAREANDGET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
	}

	public RFuture<Long> decrementAndGetAsync() {
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

	public long get() {
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

	public RFuture<Long> getAndAddAsync( long delta) {
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

	public long getAndDecrement() {
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

	public RFuture<Long> getAndDecrementAsync() {
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

	public RFuture<Long> getAndSetAsync(long newValue) {
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

	public RFuture<Long> incrementAndGetAsync() {
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

	public long getAndIncrement() {
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

	public RFuture<Long> getAndIncrementAsync() {
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

	public RFuture<Void> setAsync(long newValue) {
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
