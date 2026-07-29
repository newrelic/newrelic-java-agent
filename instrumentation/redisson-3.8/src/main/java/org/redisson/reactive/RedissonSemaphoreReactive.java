package org.redisson.reactive;

import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;
import org.redisson.api.RSemaphoreReactive;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonSemaphoreReactive implements RSemaphoreReactive {

    public Publisher<Boolean> tryAcquire() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRYACQUIRE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> tryAcquire(final int permits) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRYACQUIRE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> acquire() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ACQUIRE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> acquire(final int permits) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ACQUIRE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> release() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RELEASE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> release(final int permits) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RELEASE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> trySetPermits(final int permits) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRYSETPERMITS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> tryAcquire(final long waitTime, final TimeUnit unit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRYACQUIRE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> tryAcquire(final int permits, final long waitTime, final TimeUnit unit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRYACQUIRE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> reducePermits(final int permits) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REDUCEPERMITS");
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
