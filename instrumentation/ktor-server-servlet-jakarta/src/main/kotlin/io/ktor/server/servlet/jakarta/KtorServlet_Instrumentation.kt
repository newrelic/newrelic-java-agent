package io.ktor.server.servlet.jakarta

import com.newrelic.api.agent.*
import com.newrelic.api.agent.weaver.MatchType
import com.newrelic.api.agent.weaver.NewField
import com.newrelic.api.agent.weaver.Weave
import com.newrelic.api.agent.weaver.Weaver
import com.newrelic.labs.instrumentation.ktor.servlet.jakarta.ServletRequestHeaders
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Weave(originalName = "io.ktor.server.servlet.jakarta.KtorServlet", type = MatchType.BaseClass)
public abstract class KtorServlet_Instrumentation {

    @NewField
    private var requestToken: Token? = null

    @Trace(dispatcher = true)
    private fun asyncService(request: HttpServletRequest, response: HttpServletResponse) {
        val transaction = NewRelic.getAgent().getTransaction()
        val traced = NewRelic.getAgent().getTracedMethod()

        traced.setMetricName("Custom", "KtorServlet", "asyncService")

        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction()
        }

        // Accept distributed trace headers from incoming request
        val headers = ServletRequestHeaders(request)
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, headers)

        val uri = request.getRequestURI() ?: "Unknown"
        val method = request.getMethod() ?: "GET"
        val contentType = request.getContentType()
        val queryString = request.getQueryString()

        val txnName = getTransactionName(uri, method)
        transaction.setTransactionName(
            TransactionNamePriority.CUSTOM_HIGH,
            false,
            "KtorServlet",
            txnName
        )

        traced.addCustomAttribute("Request-URI", uri)
        traced.addCustomAttribute("Request-Method", method)
        if (contentType != null) {
            traced.addCustomAttribute("Content-Type", contentType)
        }
        if (queryString != null) {
            traced.addCustomAttribute("Query-String", queryString)
        }
        traced.addCustomAttribute("Server-Engine", "Servlet")
        traced.addCustomAttribute("Service-Type", "Async")

        if (contentType != null && contentType.startsWith("multipart/")) {
            traced.addCustomAttribute("Multipart-Request", "true")
        }

        requestToken = transaction.getToken()

        try {
            Weaver.callOriginal<Any>()
        } finally {
            if (requestToken != null) {
                if (requestToken!!.isActive()) {
                    requestToken!!.expire()
                }
                requestToken = null
            }
        }
    }

    @Trace(dispatcher = true)
    private fun blockingService(request: HttpServletRequest, response: HttpServletResponse) {
        val transaction = NewRelic.getAgent().getTransaction()
        val traced = NewRelic.getAgent().getTracedMethod()

        traced.setMetricName("Custom", "KtorServlet", "blockingService")

        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction()
        }

        // Accept distributed trace headers from incoming request
        val headers = ServletRequestHeaders(request)
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, headers)

        val uri = request.getRequestURI() ?: "Unknown"
        val method = request.getMethod() ?: "GET"
        val contentType = request.getContentType()

        val txnName = getTransactionName(uri, method)
        transaction.setTransactionName(
            TransactionNamePriority.CUSTOM_HIGH,
            false,
            "KtorServlet",
            txnName
        )

        traced.addCustomAttribute("Request-URI", uri)
        traced.addCustomAttribute("Request-Method", method)
        if (contentType != null) {
            traced.addCustomAttribute("Content-Type", contentType)
        }
        traced.addCustomAttribute("Server-Engine", "Servlet")
        traced.addCustomAttribute("Service-Type", "Blocking")

        Weaver.callOriginal<Any>()
    }

    private fun getTransactionName(uri: String, method: String): String {
        val cleanUri = if (uri.startsWith("/")) uri.substring(1) else uri
        val uriPart = if (cleanUri.isEmpty()) "Root" else cleanUri
        return "$uriPart - {$method}"
    }
}
