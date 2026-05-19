package io.ktor.server.netty.http1

import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.TransactionNamePriority
import com.newrelic.api.agent.TransportType
import com.newrelic.api.agent.weaver.Weave
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.instrumentation.labs.ktor.netty.KtorNettyHeaders
import com.newrelic.labs.instrumentation.ktor.netty.Utils
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpRequest

@Weave(originalName = "io.ktor.server.netty.http1.NettyHttp1Handler")
class NettyHttp1Handler_Instrumentation {

    @Trace
    private fun handleRequest(context: ChannelHandlerContext, message: HttpRequest) {
        val transaction = NewRelic.getAgent().transaction
        if(!transaction.isWebTransaction) {
            transaction.convertToWebTransaction()
        }
        val headers = message.headers()
        val ktorNettyHeaders = KtorNettyHeaders(headers)
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, ktorNettyHeaders)
        val txName = Utils.getTransactionName(message)
        if(txName != null && !txName.isEmpty()) {
            transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "KtorNetty", txName)
        }
        return Weaver.callOriginal()
    }
}