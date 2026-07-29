package org.redisson.reactive;

import java.util.BitSet;

import org.reactivestreams.Publisher;
import org.redisson.api.RBitSetReactive;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave
public abstract class RedissonBitSetReactive implements RBitSetReactive {

    public Publisher<Boolean> get(final long bitIndex) {
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

    public Publisher<Boolean> set(final long bitIndex, final boolean value) {
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

    public Publisher<BitSet> asBitSet() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ASBITSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Long> length() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("LENGTH");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> set(final long fromIndex, final long toIndex, final boolean value) {
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

    public Publisher<Void> clear(final long fromIndex, final long toIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CLEAR");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> set(final BitSet bs) {
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

    public Publisher<Void> not() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("NOT");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> set(final long fromIndex, final long toIndex) {
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

    public Publisher<Long> size() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SIZE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> set(final long bitIndex) {
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

    public Publisher<Long> cardinality() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CARDINALITY");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Boolean> clear(final long bitIndex) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CLEAR");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> clear() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("CLEAR");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> or(final String... bitSetNames) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("OR");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> and(final String... bitSetNames) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("AND");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public Publisher<Void> xor(final String... bitSetNames) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("XOR");
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
