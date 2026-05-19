package io.ktor.server.jetty.jakarta;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.labs.instrumentation.ktor.jetty.jakarta.Utils;
import com.newrelic.labs.instrumentation.ktor.jetty.jakarta.JettyRequestHeaders;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;

@Weave(originalName = "io.ktor.server.jetty.jakarta.JettyKtorHandler")
public abstract class JettyKtorHandler_Instrumentation {

    /**
     * This method creates the ApplicationCall and begins async processing.
     * We instrument it to create a token for tracking async work.
     * Based on Jetty 12+ API with Request, Response, and Callback parameters.
     */

    @Trace(dispatcher = true)
    public boolean handle(Request request, Response response, Callback callback) {
        TracedMethod traced = NewRelic.getAgent().getTracedMethod();
        Transaction transaction = NewRelic.getAgent().getTransaction();
        
        // Convert to web transaction if not already
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction();
        }
        
        try {
            // Extract basic request information using the new Jetty API
            String uri = request.getHttpURI().getPath();
            String method = request.getMethod();
            String scheme = request.getHttpURI().getScheme();
            String host = request.getHttpURI().getHost();
            HttpFields headers = request.getHeaders();
            String contentType = headers != null ? headers.get(HttpHeader.CONTENT_TYPE) : null;
            
            // Handle distributed tracing headers using the new Headers API
            JettyRequestHeaders jettyHeaders = new JettyRequestHeaders(headers);
            transaction.acceptDistributedTraceHeaders(TransportType.HTTP, jettyHeaders);
            
            // Use enhanced utility methods for comprehensive request tracking
            Utils.addRequestAttributes(request, traced);
            
            // Add content type if available
            if (contentType != null) {
                traced.addCustomAttribute("Content-Type", contentType);
            }
            
            // Add server and handler information
            traced.addCustomAttribute("Server-Type", "Jetty-Jakarta-3.X");
            traced.addCustomAttribute("Handler-Type", "JettyKtorHandler");
            traced.addCustomAttribute("Jetty-Version", "12+");
            
            // Handle multipart detection
            if (contentType != null && contentType.startsWith("multipart/")) {
                traced.addCustomAttribute("Multipart-Request", "true");
            }
            
            // Set enhanced transaction name using connection point data
            String transactionName = Utils.getEnhancedTransactionName(uri, method, host, scheme);
            if (transactionName != null && !transactionName.isEmpty()) {
                transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "KtorJettyJakarta33", transactionName);
            }
            
            // Add callback tracking for async operations
            traced.addCustomAttribute("Async-Handler", "true");
            
        } catch (Exception e) {
            NewRelic.noticeError(e);
        }
        
        return Weaver.callOriginal();
    }
}