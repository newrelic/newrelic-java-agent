package org.redisson;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.redisson.api.RFuture;
import org.redisson.api.RPermitExpirableSemaphore;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@SuppressWarnings("unused")
@Weave(type=MatchType.BaseClass)
public abstract class RedissonPermitExpirableSemaphore implements RPermitExpirableSemaphore {

	
    public RFuture<String> acquireAsync(long leaseTime, TimeUnit timeUnit) {
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

	private String acquire(int permits, long ttl, TimeUnit timeUnit) throws InterruptedException {
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
    
	private RFuture<String> acquireAsync(int permits, long ttl, TimeUnit timeUnit) {
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
    
    private void tryAcquireAsync(AtomicLong time, int permits, RedissonLockEntry entry, CompletableFuture<String> result, long ttl, TimeUnit timeUnit) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRYACQUIRE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }
    
    private CompletableFuture<String> acquireAsync(int permits, RedissonLockEntry entry, long ttl, TimeUnit timeUnit)  {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ACQUIRE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public RFuture<String> tryAcquireAsync() {
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

    private RFuture<String> tryAcquireAsync(int permits, long timeoutDate) {
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

    private String tryAcquire(int permits, long waitTime, long ttl, TimeUnit unit) throws InterruptedException {
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

    private RFuture<String> tryAcquireAsync(int permits, long waitTime, long ttl, TimeUnit timeUnit) {
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

    public RFuture<Boolean> tryReleaseAsync(String permitId) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRYRELEASE");
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
    
    public RFuture<Void> releaseAsync(String permitId) {
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

    public RFuture<Integer> availablePermitsAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("AVAILABLEPERMITS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> trySetPermitsAsync(int permits) {
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

    public RFuture<Void> addPermitsAsync(int permits) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDPERMITS");
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
