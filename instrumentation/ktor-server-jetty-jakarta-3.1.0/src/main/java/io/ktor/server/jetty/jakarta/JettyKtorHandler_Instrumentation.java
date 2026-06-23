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

    @Trace(dispatcher = true)
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        TracedMethod traced = NewRelic.getAgent().getTracedMethod();
        Transaction transaction = NewRelic.getAgent().getTransaction();
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction();
        }
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
        if (contentType != null) {
            traced.addCustomAttribute("Content-Type", contentType);
        }
        String transactionName = Utils.getTransactionName(uri, method);
        if (transactionName != null && !transactionName.isEmpty()) {
            transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "KtorJettyJakarta", transactionName);
        }
        Weaver.callOriginal();
    }
}