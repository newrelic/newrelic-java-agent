package org.redisson;

import java.util.Collection;
import java.util.SortedSet;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
abstract class RedissonSubSortedSet<V>  {

    public int size() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SIZE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public boolean isEmpty() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ISEMPTY");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public boolean contains(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CONTAINS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public boolean add(V e) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public boolean remove(Object o) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public boolean containsAll(Collection<?> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CONTAINSALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public boolean addAll(Collection<? extends V> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public boolean retainAll(Collection<?> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RERTAINALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public boolean removeAll(Collection<?> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVEALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public void clear() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CLEAR");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }

    public SortedSet<V> subSet(V fromElement, V toElement) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SUBSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public SortedSet<V> headSet(V toElement) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("HEADSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public SortedSet<V> tailSet(V fromElement) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TAILSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public V first() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("FIRST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    public V last() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("LAST");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

}
