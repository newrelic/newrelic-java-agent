package org.redisson.reactive;

import org.reactivestreams.Publisher;
import org.redisson.api.RAtomicLongReactive;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave
public abstract class RedissonAtomicLongReactive implements RAtomicLongReactive{


    public Publisher<Long> addAndGet(final long delta) {
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

    public Publisher<Boolean> compareAndSet(final long expect, final long update) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("COMPAREANDSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Long> decrementAndGet() {
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

    public Publisher<Long> get() {
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

    public Publisher<Long> getAndAdd(final long delta) {
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


    public Publisher<Long> getAndSet(final long newValue) {
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

    public Publisher<Long> incrementAndGet() {
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

    public Publisher<Long> getAndIncrement() {
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

    public Publisher<Long> getAndDecrement() {
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

    public Publisher<Void> set(final long newValue) {
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
