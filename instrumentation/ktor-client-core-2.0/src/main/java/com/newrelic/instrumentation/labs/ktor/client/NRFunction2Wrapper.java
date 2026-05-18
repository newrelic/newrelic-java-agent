package com.newrelic.instrumentation.labs.ktor.client;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import kotlin.jvm.functions.Function2;

public class NRFunction2Wrapper<R,S,T>  implements Function2<R,S,T> {

    private final Function2<R,S,T> delegate;
    private static boolean isTransformed = false;

    public NRFunction2Wrapper(Function2<R,S,T> delegate) {
        this.delegate = delegate;
        if (!isTransformed) {
            isTransformed = true;
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
        }
    }

    @Override
    @Trace
    public T invoke(R r, S s) {
        return delegate != null ? delegate.invoke(r, s) : null;
    }
}
