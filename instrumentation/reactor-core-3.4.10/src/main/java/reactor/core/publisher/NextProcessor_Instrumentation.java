package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;

@Weave(originalName = "reactor.core.publisher.NextProcessor")
class NextProcessor_Instrumentation<O> {

    Sinks.EmitResult tryEmitError(Throwable cause) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(cause);
        }
        return Weaver.callOriginal();
    }

    public void onError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        Weaver.callOriginal();
    }

}
