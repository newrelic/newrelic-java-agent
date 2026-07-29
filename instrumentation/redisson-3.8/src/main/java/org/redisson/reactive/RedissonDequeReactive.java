package org.redisson.reactive;

import org.reactivestreams.Publisher;
import org.redisson.api.RDequeReactive;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonDequeReactive<V> implements RDequeReactive<V> {

    public Publisher<Void> addFirst(V e) {
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

    public Publisher<Void> addLast(V e) {
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

    public Publisher<V> getLast() {
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

    public Publisher<Boolean> offerFirst(V e) {
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

    public Publisher<Integer> offerLast(V e) {
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

    public Publisher<V> peekFirst() {
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

    public Publisher<V> peekLast() {
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

    public Publisher<V> pollFirst() {
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

    public Publisher<V> pollLast() {
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

    public Publisher<V> pop() {
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

    public Publisher<Void> push(V e) {
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

    public Publisher<Boolean> removeFirstOccurrence(Object o) {
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

    public Publisher<V> removeFirst() {
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

    public Publisher<V> removeLast() {
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

    public Publisher<Boolean> removeLastOccurrence(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("removeLastOccurrence".toUpperCase());
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
