package reactor.core.publisher;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "reactor.core.publisher.SinkOneSerialized")
public abstract class SinkOneSerialized_Instrumentation<T>  extends  SinkEmptySerialized_Instrumentation<T>{

    @Trace(excludeFromTransactionTrace = true)
    public Sinks.EmitResult tryEmitValue(T t) {
        return Weaver.callOriginal();
    }
}
