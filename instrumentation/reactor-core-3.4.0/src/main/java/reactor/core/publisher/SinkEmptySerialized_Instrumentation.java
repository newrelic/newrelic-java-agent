package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;

@Weave(originalName = "reactor.core.publisher.SinkEmptySerialized", type = MatchType.BaseClass)
class SinkEmptySerialized_Instrumentation<T> {

    @Trace
    public Sinks.EmitResult tryEmitEmpty() {
        return Weaver.callOriginal();
    }

    @Trace
    public Sinks.EmitResult tryEmitError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        return Weaver.callOriginal();
    }
}
