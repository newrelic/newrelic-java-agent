package io.micronaut.inject;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "io.micronaut.inject.ExecutionHandle", type = MatchType.Interface)
public abstract class ExecutionHandle_Instrumentation<T, R> {
    public abstract Class getDeclaringType();

    @Trace
    public R invoke(Object... arguments) {
        return Weaver.callOriginal();
    }
}