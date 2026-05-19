package io.ktor.server.jetty.jakarta;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.labs.instrumentation.ktor.jetty.jakarta.KtorJettyHeaders;
import com.newrelic.labs.instrumentation.ktor.jetty.jakarta.Utils;
import org.eclipse.jetty.server.Request;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Weave(originalName = "io.ktor.server.jetty.jakarta.JettyKtorHandler")
public abstract class JettyKtorHandler_Instrumentation {

    /**
     * This method creates the ApplicationCall and begins async processing.
     * We instrument it to create a token for tracking async work.
     */

    @Trace(dispatcher = true)
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        TracedMethod traced = NewRelic.getAgent().getTracedMethod();
        Transaction transaction = NewRelic.getAgent().getTransaction();
        
        // Convert to web transaction if not already
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction();
        }
        
        // Handle distributed tracing headers
        KtorJettyHeaders jettyHeaders = new KtorJettyHeaders(request);
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, jettyHeaders);
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String contentType = request.getContentType();
        
        if (uri != null) {
            traced.addCustomAttribute("Request-URI", uri);
        }
        if (method != null) {
            traced.addCustomAttribute("Request-Method", method);
        }
        if (target != null) {
            traced.addCustomAttribute("Target", target);
        }
        if (contentType != null) {
            traced.addCustomAttribute("Content-Type", contentType);
        }
        
        // Add Jetty-specific attributes based on actual implementation
        traced.addCustomAttribute("Server-Type", "Jetty-Jakarta-3.1");
        traced.addCustomAttribute("Handler-Type", "JettyKtorHandler");
        
        // Handle multipart detection (from actual source)
        if (contentType != null && contentType.startsWith("multipart/")) {
            traced.addCustomAttribute("Multipart-Request", "true");
        }
        
        // Set transaction name with priority (similar to Netty pattern)
        String transactionName = Utils.getTransactionName(uri, method);
        if (transactionName != null && !transactionName.isEmpty()) {
            transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "KtorJettyJakarta3", transactionName);
        }
        
        Weaver.callOriginal();
    }
}