package ratpack.exec.internal;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import nr.ratpack.instrumentation.RatpackUtil;
import ratpack.func.Action;

@Weave(originalName = "ratpack.exec.internal.DefaultExecution")
public class DefaultExecution_Instrumentation {

    public void delimit(Action<? super Throwable> onError, Action<? super Continuation> segment) {
        if (AgentBridge.getAgent().getTransaction(false) != null) {
            RatpackUtil.storeTokenForContinuation(segment);
        }
        Weaver.callOriginal();
    }

}
