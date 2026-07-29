package org.redisson;

import org.redisson.api.RDeque;
import org.redisson.api.RFuture;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonDeque<V> implements RDeque<V> {

    
    public RFuture<Void> addFirstAsync(V e) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Void> addLastAsync(V e) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDLAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }


    public RFuture<V> getLastAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETLAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Boolean> offerFirstAsync(V e) {
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

    
    public RFuture<Boolean> offerLastAsync(V e) {
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

    
    public RFuture<V> peekFirstAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PEEKFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<V> peekLastAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PEEKLAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<V> pollFirstAsync() {
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

    
    public V pollFirst() {
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

    
    public RFuture<V> pollLastAsync() {
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
    
    public RFuture<V> popAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POP");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public V pop() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("POP");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Void> pushAsync(V e) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUSH");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public void push(V e) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUSH");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }

    
    public RFuture<Boolean> removeFirstOccurrenceAsync(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVEFIRSTOCCURENCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public boolean removeFirstOccurrence(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVEFIRSTOCCURENCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<V> removeFirstAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVEFIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<V> removeLastAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVELAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public RFuture<Boolean> removeLastOccurrenceAsync(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVELASTOCCURENCE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    
    public boolean removeLastOccurrence(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVELASTOCCURENCE");
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
