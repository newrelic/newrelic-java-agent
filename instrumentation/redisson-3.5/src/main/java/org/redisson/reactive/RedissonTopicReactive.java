package org.redisson.reactive;

import org.reactivestreams.Publisher;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonTopicReactive<M> {

    public Publisher<Long> publish(M message) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("PUBLISH");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }

}
