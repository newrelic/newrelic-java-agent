package io.ktor.util.pipeline

import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.weaver.MatchType
import com.newrelic.api.agent.weaver.SkipIfPresent
import com.newrelic.api.agent.weaver.Weave
import com.newrelic.api.agent.weaver.Weaver

@SkipIfPresent(originalName = "io.ktor.util.pipeline.PipelineExecutor")
class PipelineExecutor_Instrumentation<R> {

}