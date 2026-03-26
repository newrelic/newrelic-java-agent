package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;
import reactor.core.CorePublisher;

@Weave(originalName = "reactor.core.publisher.NextProcessor")
class NextProcessor_Instrumentation<O> {

    @NewField
    private Token   token;

    NextProcessor_Instrumentation(CorePublisher<? extends O> source) {
        token = NewRelic.getAgent().getTransaction().getToken();
    }

    @Trace(async = true)
    public Sinks.EmitResult tryEmitError(Throwable cause) {
        if(token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public Sinks.EmitResult tryEmitValue(O value) {
        token.linkAndExpire();
        token = null;
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    public Sinks.EmitResult tryEmitEmpty() {
        token.linkAndExpire();
        token = null;
        return Weaver.callOriginal();
    }

    public void onError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        Weaver.callOriginal();
    }

}
