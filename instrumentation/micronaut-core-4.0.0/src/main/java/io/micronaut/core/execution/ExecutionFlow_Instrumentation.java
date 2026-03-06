package io.micronaut.core.execution;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.*;
import com.nr.agent.instrumentation.micronaut.NRBiConsumerTokenWrapper;
import com.nr.agent.instrumentation.micronaut.NRBiConsumerWrapper;

import java.util.function.BiConsumer;

@Weave(originalName = "io.micronaut.core.execution.ExecutionFlow", type = MatchType.Interface)
public class ExecutionFlow_Instrumentation<T> {

    @NewField
    private Token token;

    @WeaveAllConstructors
    public ExecutionFlow_Instrumentation() {
        Token t = NewRelic.getAgent().getTransaction().getToken();
        if (t != null && t.isActive()) {
            token = t;
        } else if(token != null) {
            t.expire();
            t = null;
        }
    }

    @Trace(async=true)
    public ImperativeExecutionFlow<T> tryComplete() {
        if(token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    }

    public void onComplete(BiConsumer<? super T, Throwable> fn) {
        if(!(fn instanceof NRBiConsumerWrapper)) {
            fn = new NRBiConsumerTokenWrapper<>(fn);
        }
        Weaver.callOriginal();
    }
}
