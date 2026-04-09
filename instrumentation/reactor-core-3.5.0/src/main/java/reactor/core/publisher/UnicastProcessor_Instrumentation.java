package reactor.core.publisher;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.reactor.ReactorConfig;
import reactor.core.Disposable;

import java.util.Queue;
import java.util.function.Consumer;

@Weave(originalName = "reactor.core.publisher.UnicastProcessor")
public class UnicastProcessor_Instrumentation<T> {

    @Trace
    public Sinks.EmitResult tryEmitComplete() {
        return Weaver.callOriginal();
    }

    @Trace
    public Sinks.EmitResult tryEmitError(Throwable t) {
        return Weaver.callOriginal();
    }

    @Trace
    public Sinks.EmitResult tryEmitNext(T t) {
        return Weaver.callOriginal();
    }

    public void onError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        Weaver.callOriginal();
    }

}
