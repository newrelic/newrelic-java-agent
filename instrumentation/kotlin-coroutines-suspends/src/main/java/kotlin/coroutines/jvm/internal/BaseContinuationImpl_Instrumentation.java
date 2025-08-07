package kotlin.coroutines.jvm.internal;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "kotlin.coroutines.jvm.internal.BaseContinuationImpl", type = MatchType.BaseClass)
public abstract class BaseContinuationImpl_Instrumentation {

    protected java.lang.Object invokeSuspend(java.lang.Object result) {

        return Weaver.callOriginal();
    }
}
