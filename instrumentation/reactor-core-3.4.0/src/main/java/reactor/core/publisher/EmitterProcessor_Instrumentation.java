package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;

@Weave(originalName = "reactor.core.publisher.EmitterProcessor")
public class EmitterProcessor_Instrumentation<T> {

    public Sinks.EmitResult tryEmitError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        return Weaver.callOriginal();
    }

    public void onError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        Weaver.callOriginal();
    }

}
