package org.redisson;

import org.redisson.api.RFuture;
import org.redisson.api.RTopic;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonTopic implements RTopic {

    public RFuture<Long> publishAsync(Object message) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUBLISH");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

}
