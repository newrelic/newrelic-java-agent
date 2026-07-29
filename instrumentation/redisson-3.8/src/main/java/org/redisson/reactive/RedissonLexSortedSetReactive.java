package org.redisson.reactive;

import java.util.Collection;

import org.reactivestreams.Publisher;
import org.redisson.api.RLexSortedSetReactive;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave
public abstract class RedissonLexSortedSetReactive implements RLexSortedSetReactive {

    public Publisher<Integer> addAll(Publisher<? extends String> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> removeRangeHead(final String toElement, final boolean toInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVERANGEHEAD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> removeRangeTail(final String fromElement, final boolean fromInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVERANGETAIL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> removeRange(final String fromElement, final boolean fromInclusive, final String toElement, final boolean toInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVERANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<String>> rangeHead(final String toElement, final boolean toInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RANGEHEAD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<String>> rangeTail(final String fromElement, final boolean fromInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RANGETAIL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<String>> range(final String fromElement, final boolean fromInclusive, final String toElement, final boolean toInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<String>> rangeHead(final String toElement, final boolean toInclusive, final int offset, final int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RANGEHEAD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<String>> rangeTail(final String fromElement, final boolean fromInclusive, final int offset, final int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RANGETAIL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<String>> range(final String fromElement, final boolean fromInclusive, final String toElement, final boolean toInclusive, final int offset, final int count) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RANGE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> countTail(final String fromElement, final boolean fromInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("COUNTTAIL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> countHead(final String toElement, final boolean toInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("COUNTHEAD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> count(final String fromElement, final boolean fromInclusive, final String toElement, final boolean toInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("COUNT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> add(final String e) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADD");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Integer> addAll(Collection<? extends String> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Collection<String>> range(int startIndex, int endIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RANGE");
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
