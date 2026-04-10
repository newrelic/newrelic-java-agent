package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;

@Weave(originalName = "reactor.core.publisher.NextProcessor")
class NextProcessor_Instrumentation<O> {

    @Trace(excludeFromTransactionTrace = true)
    public Sinks.EmitResult tryEmitError(Throwable cause) {
        return Weaver.callOriginal();
    }

    @Trace(excludeFromTransactionTrace = true)
    public Sinks.EmitResult tryEmitValue(O value) {
        return Weaver.callOriginal();
    }

    @Trace(excludeFromTransactionTrace = true)
    public Sinks.EmitResult tryEmitEmpty() {
        return Weaver.callOriginal();
    }

    public void onError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        Weaver.callOriginal();
    }

}
