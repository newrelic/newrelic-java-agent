package reactor.core.publisher;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "reactor.core.publisher.SinkOneMulticast")
class SinkOneMulticast_Instrumentation<O> extends SinkEmptyMulticast_Instrumentation<O> {

    @Trace(async=true)
    public Sinks.EmitResult tryEmitValue(O value) {
        if(token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();

    }
}
