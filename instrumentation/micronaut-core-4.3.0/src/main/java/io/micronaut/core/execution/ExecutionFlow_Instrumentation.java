package io.micronaut.core.execution;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.*;

import java.util.function.BiConsumer;

@Weave(originalName = "io.micronaut.core.execution.ExecutionFlow", type = MatchType.Interface)
public class ExecutionFlow_Instrumentation<T> {

    @Trace
    public ImperativeExecutionFlow<T> tryComplete() {
        return Weaver.callOriginal();
    }

    @Trace
    public T tryCompleteValue() {
        return Weaver.callOriginal();
    }

    @Trace
    public Throwable tryCompleteError() {
        return Weaver.callOriginal();
    }

    @Trace
    public void onComplete(BiConsumer<? super T, Throwable> fn) {
        Weaver.callOriginal();
    }
}
