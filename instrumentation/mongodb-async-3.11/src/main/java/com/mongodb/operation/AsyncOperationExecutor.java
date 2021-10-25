package com.mongodb.operation;

import com.mongodb.ReadPreference;
import com.mongodb.async.SingleResultCallback;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type=MatchType.Interface)
public abstract class AsyncOperationExecutor {

	@Trace
	public <T> void execute(AsyncReadOperation<T> operation, ReadPreference readPreference, SingleResultCallback<T> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","AsyncOperationExecutor","Read","execute"});
		Weaver.callOriginal();
	}
	
	@Trace
	public <T> void execute(AsyncWriteOperation<T> operation, SingleResultCallback<T> callback) {
		NewRelic.getAgent().getTracedMethod().setMetricName(new String[] {"Custom","AsyncOperationExecutor","Write","execute"});
		Weaver.callOriginal();
	}
}
