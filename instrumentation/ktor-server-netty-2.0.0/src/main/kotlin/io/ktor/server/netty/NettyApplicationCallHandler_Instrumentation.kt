package io.ktor.server.netty

import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.TransactionNamePriority
import com.newrelic.api.agent.TransportType
import com.newrelic.api.agent.weaver.Weave
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.instrumentation.labs.ktor.netty.KtorNettyHeaders
import com.newrelic.labs.instrumentation.ktor.netty.Utils
import io.ktor.server.application.*
import io.netty.channel.ChannelHandlerContext

@Weave(originalName = "io.ktor.server.netty.NettyApplicationCallHandler")
class NettyApplicationCallHandler_Instrumentation {

    @Trace
    private fun handleRequest(context: ChannelHandlerContext, call: ApplicationCall) {
        val appName = Utils.getApplicationName(call.application)
        if(!appName.isNullOrBlank()) {
            NewRelic.getAgent().tracedMethod.addCustomAttribute("appName", appName)
        }
        val transaction = NewRelic.getAgent().transaction
        if(!transaction.isWebTransaction) {
            transaction.convertToWebTransaction()
        }
        val request = call.request
        val headers = request.headers
        val ktorNettyHeaders = KtorNettyHeaders(headers)
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, ktorNettyHeaders)
        val point = request.local
        val txName = Utils.getTransactionName(point)
        if(txName != null && !txName.isEmpty()) {
            transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "KtorNetty", txName)
        }
        return Weaver.callOriginal()
    }
}