package org.redisson;

import org.redisson.api.RFuture;
import org.redisson.api.RPriorityDeque;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonPriorityDeque<V> implements RPriorityDeque<V> {

	public RFuture<V> getLastAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GETASYNC");
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


	public RFuture<Boolean> removeFirstOccurrenceAsync(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("removeFirstOccurrence".toUpperCase());
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
			Utils.setOperation("removeFirstOccurrence".toUpperCase());
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
			Utils.setOperation("REMOVELASTOCCURRENCE");
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
			Utils.setOperation("REMOVELASTOCCURRENCE");
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
