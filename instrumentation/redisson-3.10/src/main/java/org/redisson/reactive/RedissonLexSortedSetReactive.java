package org.redisson.reactive;

import org.reactivestreams.Publisher;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave
public abstract class RedissonLexSortedSetReactive {

    public Publisher<Boolean> addAll(Publisher<? extends String> c) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ADDALL");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		return Weaver.callOriginal();
    }



}
