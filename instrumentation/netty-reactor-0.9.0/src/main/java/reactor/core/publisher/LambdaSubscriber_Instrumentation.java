package reactor.core.publisher;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import reactor.util.context.Context;

@Weave(originalName = "reactor.core.publisher.LambdaSubscriber")
abstract class LambdaSubscriber_Instrumentation {
    final Context initialContext = Weaver.callOriginal();
    @NewField
    private Context nrContext;

    @WeaveAllConstructors
    protected LambdaSubscriber_Instrumentation() {
        if (AgentBridge.getAgent().getTransaction(false) != null
                && initialContext.getOrDefault("newrelic-token", null) == null) {
            nrContext = Context.of("newrelic-token", NewRelic.getAgent().getTransaction().getToken());
        }
    }

    public final void onComplete() {
        Token token = this.currentContext().getOrDefault("newrelic-token", null);
        if (token != null) {
            token.expire();
            token = null;
            this.nrContext = null;

        }
    }

    public Context currentContext() {
        if (nrContext != null) {
            //return nrContext;
            return initialContext.putAll(nrContext);
        }
        return Weaver.callOriginal();
    }

}
