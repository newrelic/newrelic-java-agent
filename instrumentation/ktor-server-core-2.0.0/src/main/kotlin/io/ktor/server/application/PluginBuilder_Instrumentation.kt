package io.ktor.server.application

import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.weaver.MatchType
import com.newrelic.api.agent.weaver.Weave
import com.newrelic.api.agent.weaver.Weaver

@Weave(type = MatchType.BaseClass, originalName = "io.ktor.server.application.PluginBuilder")
public abstract class PluginBuilder_Instrumentatio<PluginConfig: Any> {

    @Trace
    public fun onCall(block: suspend OnCallContext<PluginConfig>.(call: ApplicationCall) -> Unit) {
        return Weaver.callOriginal()
    }

    @Trace
    public fun onCallReceive(
        block: suspend OnCallReceiveContext<PluginConfig>.(call: ApplicationCall, body: Any) -> Unit
    ) {
        return Weaver.callOriginal()
    }

    @Trace
    public fun onCallRespond(
        block: suspend OnCallRespondContext<PluginConfig>.(call: ApplicationCall, body: Any) -> Unit
    ) {
        return Weaver.callOriginal()
    }

    @Trace
    public fun <HookHandler> on(
        hook: Hook<HookHandler>,
        handler: HookHandler
    ) {
        return Weaver.callOriginal()
    }

    @Trace
    public fun onCallReceive(
        block: suspend OnCallReceiveContext<PluginConfig>.(call: ApplicationCall) -> Unit
    ) {
        return Weaver.callOriginal()
    }

    @Trace
    public fun onCallRespond(
        block: suspend OnCallRespondContext<PluginConfig>.(call: ApplicationCall) -> Unit
    ) {
        return Weaver.callOriginal()
    }
}