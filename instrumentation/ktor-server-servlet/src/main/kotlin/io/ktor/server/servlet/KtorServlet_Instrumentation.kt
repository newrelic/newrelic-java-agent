package io.ktor.server.servlet

import com.newrelic.api.agent.NewRelic
import com.newrelic.api.agent.Trace
import com.newrelic.api.agent.TransactionNamePriority
import com.newrelic.api.agent.TransportType
import com.newrelic.api.agent.weaver.MatchType
import com.newrelic.api.agent.weaver.Weave
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.labs.instrumentation.ktor.servlet.ServletRequestHeaders
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Weave(originalName = "io.ktor.server.servlet.KtorServlet", type = MatchType.BaseClass)
public abstract class KtorServlet_Instrumentation {

    @Trace(dispatcher = true)
    private fun asyncService(request: HttpServletRequest, response: HttpServletResponse) {
        val transaction = NewRelic.getAgent().getTransaction()
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction()
        }
        val headers = ServletRequestHeaders(request)
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, headers)
        val uri = request.getRequestURI() ?: "Unknown"
        val method = request.getMethod() ?: "GET"
        transaction.setTransactionName(
            TransactionNamePriority.CUSTOM_LOW,
            false,
            "KtorServlet",
            getTransactionName(uri, method)
        )
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Request-URI", uri)
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Request-Method", method)
        Weaver.callOriginal<Any>()
    }

    @Trace(dispatcher = true)
    private fun blockingService(request: HttpServletRequest, response: HttpServletResponse) {
        val transaction = NewRelic.getAgent().getTransaction()
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction()
        }
        val headers = ServletRequestHeaders(request)
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, headers)
        val uri = request.getRequestURI() ?: "Unknown"
        val method = request.getMethod() ?: "GET"
        transaction.setTransactionName(
            TransactionNamePriority.CUSTOM_LOW,
            false,
            "KtorServlet",
            getTransactionName(uri, method)
        )
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Request-URI", uri)
        NewRelic.getAgent().getTracedMethod().addCustomAttribute("Request-Method", method)
        Weaver.callOriginal<Any>()
    }

    private fun getTransactionName(uri: String, method: String): String {
        val cleanUri = if (uri.startsWith("/")) uri.substring(1) else uri
        val uriPart = if (cleanUri.isEmpty()) "Root" else cleanUri
        return "$uriPart - {$method}"
    }
}
