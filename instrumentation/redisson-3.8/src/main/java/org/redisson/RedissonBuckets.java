package org.redisson;

import java.util.List;
import java.util.Map;

import org.redisson.api.RBucket;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonBuckets {

    
    public <V> Map<String, V> get(String... keys) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("GET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    
    public boolean trySet(Map<String, ?> buckets) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TRYSET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

    
    public void set(Map<String, ?> buckets) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("SET");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		Weaver.callOriginal();
    }


}
