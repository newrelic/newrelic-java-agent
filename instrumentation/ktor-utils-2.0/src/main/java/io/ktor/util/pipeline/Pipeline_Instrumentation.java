package io.ktor.util.pipeline;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.ktor.utils.PipelineUtils;
import kotlin.coroutines.Continuation;

@Weave(type = MatchType.BaseClass, originalName = "io.ktor.util.pipeline.Pipeline")
public class Pipeline_Instrumentation<TSubject, TContext> {

    @NewField
    private Token token = null;

    public Pipeline_Instrumentation(PipelinePhase... phases) {
        String simpleName = getClass().getSimpleName();
        if(PipelineUtils.tracePipeline(simpleName)) {
            token = NewRelic.getAgent().getTransaction().getToken();
        }
    }

    @Trace(async = true)
    public Object execute(TContext context, TSubject subject, Continuation<? super TSubject>  continuation) {
        if(token != null) {
            token.linkAndExpire();
            token = null;
        }
        return Weaver.callOriginal();
    };



}
