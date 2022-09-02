package ratpack.exec.internal;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackUtil;
import ratpack.exec.Downstream;
import ratpack.exec.Promise;
import ratpack.exec.Upstream;
import ratpack.func.Action;
import ratpack.func.Function;

@Weave(type = MatchType.ExactClass, originalName = "ratpack.exec.internal.DefaultPromise")
public class DefaultPromise_Instrumentation<T> {

    public DefaultPromise_Instrumentation(Upstream<T> upstream) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();
        if (token.isActive()) {
            RatpackUtil.storeTokenForPromise(this, token);
        }
    }

    @Trace(async = true)
    public void then(Action<? super T> then) {
        final Token token = RatpackUtil.getAndRemoveTokenForPromise(this);
        if (token != null) {
            token.linkAndExpire();
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    public void connect(Downstream<? super T> downstream) {
        final Token token = RatpackUtil.getAndRemoveTokenForPromise(this);
        if (token != null) {
            token.linkAndExpire();
        }
        Weaver.callOriginal();
    }

    @Trace(async = true)
    public <O> Promise<O> transform(Function<? super Upstream<? extends T>, ? extends Upstream<O>> upstreamTransformer) {
        final Token token = RatpackUtil.getAndRemoveTokenForPromise(this);
        if (token != null) {
            token.linkAndExpire();
        }
        return Weaver.callOriginal();
    }
}
