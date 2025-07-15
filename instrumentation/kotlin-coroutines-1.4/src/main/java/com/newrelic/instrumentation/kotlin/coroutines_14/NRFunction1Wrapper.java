package com.newrelic.instrumentation.kotlin.coroutines_14;

import com.newrelic.agent.bridge.AgentBridge;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.jvm.internal.SuspendFunction;
import kotlin.jvm.functions.Function1;

public class NRFunction1Wrapper<P1, R> implements Function1<P1, R> {
    
    private Function1<P1, R> delegate = null;
    private static boolean isTransformed = false;
    
    public NRFunction1Wrapper(Function1<P1,R> d) {
        delegate = d;
        if(!isTransformed) {
            AgentBridge.instrumentation.retransformUninstrumentedClass(getClass());
            isTransformed = true;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public R invoke(P1 p1) {
        if(p1 instanceof Continuation && !(p1 instanceof SuspendFunction)) {
            // wrap if needed
            if(!(p1 instanceof NRContinuationWrapper)) {
                String cont_string = Utils.getContinuationString((Continuation)p1);
                NRContinuationWrapper wrapper = new NRContinuationWrapper<>((Continuation)p1, cont_string);
                p1 = (P1) wrapper;
            }
        }
        return delegate != null ? delegate.invoke(p1) : null;
    }

}
