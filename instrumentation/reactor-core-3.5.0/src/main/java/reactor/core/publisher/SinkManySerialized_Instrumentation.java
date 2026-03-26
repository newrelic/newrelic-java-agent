package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "reactor.core.publisher.SinkManySerialized")
class SinkManySerialized_Instrumentation<T> {

    @NewField
    private Token token;

    SinkManySerialized_Instrumentation(Sinks.Many<T> sink, ContextHolder contextHolder) {
        token = NewRelic.getAgent().getTransaction().getToken();
    }

    @Trace(async = true)
    public Sinks.EmitResult tryEmitComplete() {
        if(token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public Sinks.EmitResult tryEmitError(Throwable t) {
        if(token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public Sinks.EmitResult tryEmitNext(T t) {
        if(token != null) {
            token.link();
        }
        return Weaver.callOriginal();
    }

}
