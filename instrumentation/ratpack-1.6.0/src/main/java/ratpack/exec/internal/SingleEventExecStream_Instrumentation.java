package ratpack.exec.internal;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackUtil;
import ratpack.func.Action;

@Weave(originalName = "ratpack.exec.internal.DefaultExecution$SingleEventExecStream")
class SingleEventExecStream_Instrumentation {
    Action<? super Continuation> initial = Weaver.callOriginal();

    @Trace(async = true, excludeFromTransactionTrace = true)
    boolean exec() throws Exception {
        if (initial != null) {
            Token token = RatpackUtil.getTokenForContinuation(initial);
            if (token != null) {
                token.linkAndExpire();
            }
        }

        return Weaver.callOriginal();
    }
}
