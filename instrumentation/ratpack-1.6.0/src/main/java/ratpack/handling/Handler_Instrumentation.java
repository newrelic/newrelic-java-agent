package ratpack.handling;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

@Weave(originalName = "ratpack.handling.Handler", type = MatchType.Interface)
public abstract class Handler_Instrumentation {

    @Trace
    public abstract void handle(Context ctx) throws Exception;
}
