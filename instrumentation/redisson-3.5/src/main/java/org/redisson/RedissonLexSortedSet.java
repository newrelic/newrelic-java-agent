package org.redisson;

import java.util.Collection;

import org.redisson.api.RFuture;
import org.redisson.api.RLexSortedSet;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonLexSortedSet implements RLexSortedSet {

    public RFuture<Integer> removeRangeHeadAsync(String toElement, boolean toInclusive) {
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
    
    
    public RFuture<Integer> removeRangeTailAsync(String fromElement, boolean fromInclusive) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("REMOVERANGETOTAIL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    
    public RFuture<Integer> removeRangeAsync(String fromElement, boolean fromInclusive, String toElement,
            boolean toInclusive) {
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
    
    
    public RFuture<Collection<String>> rangeHeadAsync(String toElement, boolean toInclusive) {
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
    
    
    public RFuture<Collection<String>> rangeTailAsync(String fromElement, boolean fromInclusive) {
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
    
    
    public RFuture<Collection<String>> rangeAsync(String fromElement, boolean fromInclusive, String toElement,
            boolean toInclusive) {
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

    public RFuture<Collection<String>> rangeHeadAsync(String toElement, boolean toInclusive, int offset, int count) {
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

    public RFuture<Collection<String>> rangeTailAsync(String fromElement, boolean fromInclusive, int offset, int count) {
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
    
    
    public RFuture<Collection<String>> rangeAsync(String fromElement, boolean fromInclusive, String toElement,
            boolean toInclusive, int offset, int count) {
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
    
    
    public RFuture<Integer> countTailAsync(String fromElement, boolean fromInclusive) {
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

    public RFuture<Integer> countHeadAsync(String toElement, boolean toInclusive) {
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

    public RFuture<Integer> countAsync(String fromElement, boolean fromInclusive, String toElement,
            boolean toInclusive) {
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
    
    public RFuture<Boolean> addAsync(String e) {
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

    
    public RFuture<Boolean> addAllAsync(Collection<? extends String> c) {
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

    
    public Collection<String> range(int startIndex, int endIndex) {
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
    
    
    public RFuture<Collection<String>> rangeAsync(int startIndex, int endIndex) {
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
