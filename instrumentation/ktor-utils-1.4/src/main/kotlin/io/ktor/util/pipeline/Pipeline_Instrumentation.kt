package io.ktor.util.pipeline

import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.weaver.MatchType
import com.newrelic.api.agent.weaver.Weave
import com.newrelic.api.agent.weaver.Weaver

@Weave(type = MatchType.BaseClass, originalName = "io.ktor.util.pipeline.Pipeline")
open class Pipeline_Instrumentation<TSubject : Any, TContext : Any> {

    @Trace
    suspend fun execute(context : TContext, subject : TSubject) : TSubject  {
        NewRelic.getAgent().tracedMethod.setMetricName("Custom","Ktor-Utils","Pipeline",this.javaClass.name,"execute")
        return Weaver.callOriginal()
    }

    @Trace
    fun intercept(phase: PipelinePhase, block: PipelineInterceptor<TSubject, TContext>) {
        val phaseName = phase.name
        NewRelic.getAgent().tracedMethod.setMetricName("Custom","Ktor-Utils","Pipeline",this.javaClass.simpleName,"intercept",phaseName)
        return Weaver.callOriginal()
    }
}

typealias PipelineInterceptor<TSubject, TContext> = suspend PipelineContext<TSubject, TContext>.(TSubject) -> Unit