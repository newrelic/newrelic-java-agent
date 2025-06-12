package kotlinx.coroutines;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type=MatchType.BaseClass)
public abstract class CompletionHandlerBase {

	@Trace
	public abstract void invoke(Throwable t);
}
