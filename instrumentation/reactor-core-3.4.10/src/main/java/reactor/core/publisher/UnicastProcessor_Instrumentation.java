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

    @NewField
    private Token token;

    public UnicastProcessor_Instrumentation(Queue<T> queue) {
        if(token == null) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }
    }

    public UnicastProcessor_Instrumentation(Queue<T> queue, Disposable onTerminate) {
        if(token == null) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }
    }

    public UnicastProcessor_Instrumentation(Queue<T> queue, Consumer<? super T> onOverflow, Disposable onTerminate) {
        if(token == null) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }
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
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
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

    public void onError(Throwable t) {
        if(ReactorConfig.errorsEnabled) {
            NewRelic.noticeError(t);
        }
        Weaver.callOriginal();
    }

}
