package com.newrelic.weave.weavepackage.testclasses;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass, originalName = "kotlin.coroutines.jvm.internal.BaseContinuationImpl")
class Weave_SampleCoroutine {
    public Object invokeSuspend(Object obj) {
        return Weaver.callOriginal();
    }
}
