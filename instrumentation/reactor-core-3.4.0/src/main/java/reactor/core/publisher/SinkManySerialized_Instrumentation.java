package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;

@Weave(originalName = "reactor.core.publisher.SinkManySerialized")
class SinkManySerialized_Instrumentation<T> {

    @Trace(async = true)
    public Sinks.EmitResult tryEmitComplete() {
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public Sinks.EmitResult tryEmitError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public Sinks.EmitResult tryEmitNext(T t) {
        return Weaver.callOriginal();
    }

}
