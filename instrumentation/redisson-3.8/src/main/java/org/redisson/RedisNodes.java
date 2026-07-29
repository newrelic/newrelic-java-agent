package org.redisson;

import org.redisson.api.Node;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class RedisNodes<N extends Node> {

	@Trace
	public boolean pingAll() {
		DatastoreParameters params = DatastoreParameters.product("Redisson").collection("All").operation("ping").build();
		NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
		return Weaver.callOriginal();
	}
}
