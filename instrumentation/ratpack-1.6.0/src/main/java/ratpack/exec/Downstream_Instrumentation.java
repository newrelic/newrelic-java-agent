package ratpack.exec;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "ratpack.exec.Downstream", type = MatchType.Interface)
public class Downstream_Instrumentation {

    @NewField
    public Token token;

    @WeaveAllConstructors
    Downstream_Instrumentation() {
        Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }
    }

    /*
     * From Downstream.java:
     *
     * Once connected, an upstream will invoke only one of either the {@link #success},
     * {@link #error} or {@link #complete} methods exactly once.
     *
     *
     * Also, we're instrumenting a functional interface. The metric name
     * of the things that actually end up instrumenting is a bit ugly
     * e.g., DefaultPromise$1.success
     *
     * Hence the metric name in success, error, and complete.
     */

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void success(Object value) {
        if (token != null) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Downstream.success()");
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void error(Throwable t) {
        if (token != null) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Downstream.error()");
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }


    @Trace(async = true, excludeFromTransactionTrace = true)
    public void complete() {
        if (token != null) {
            NewRelic.getAgent().getTracedMethod().setMetricName("Downstream.complete()");
            token.linkAndExpire();
            token = null;
        }
        Weaver.callOriginal();
    }

}
