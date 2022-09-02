package ratpack.handling.internal;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackRequest;
import nr.ratpack.instrumentation.RatpackUtil;
import ratpack.func.Action;
import ratpack.handling.RequestOutcome;
import ratpack.http.Request;

@Weave(originalName = "ratpack.handling.internal.DefaultContext")
public abstract class DefaultContext_Instrumentation {

    public DefaultContext_Instrumentation(DefaultContext.RequestConstants requestConstants) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            // Token is used to link first call to DefaultContext.next()
            // and to link error handlers.
            // it is expired in the closeable registered below.
            final Token token = NewRelic.getAgent().getTransaction().getToken();
            RatpackUtil.storeTokenForContext(this, token);
            this.onClose(RatpackUtil.expireTokenAction(this));
            NewRelic.getAgent().getTransaction().setWebRequest(new RatpackRequest(getRequest()));
        }
    }

    // We add excludeFromTransactionTrace here to only show the first call
    // All other calls will be traced by Handler_Instrumentation
    @Trace(async = true, excludeFromTransactionTrace = true)
    public void next() {
        final Token token = RatpackUtil.getTokenForContext(this);
        if (token != null) {
            NewRelic.getAgent().getTracedMethod().setMetricName("DefaultContext.next()");
            token.link();
        }
        Weaver.callOriginal();
    }

    public abstract Request getRequest();

    @Trace(async = true)
    public void error(Throwable throwable) {
        final Token token = RatpackUtil.getTokenForContext(this);
        if (token != null) {
            token.link();
        }
        Weaver.callOriginal();
    }

    public abstract void onClose(Action<? super RequestOutcome> callback);
}
