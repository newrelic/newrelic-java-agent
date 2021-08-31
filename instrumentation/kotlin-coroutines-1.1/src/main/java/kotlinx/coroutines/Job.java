package kotlinx.coroutines;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Weave(type=MatchType.Interface)
public abstract class Job {

	@Trace
	public abstract void cancel();
	
	@Trace
	public boolean cancel(Throwable t) {
		NewRelic.noticeError(t);
		return Weaver.callOriginal();
	}
	
	@Trace
	public abstract Object join(Continuation<? super Unit> c);
	

}
