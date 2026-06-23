package io.ktor.server.cio;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import io.ktor.http.cio.Request;
import io.ktor.server.cio.backend.ServerRequestScope;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Weave(originalName = "io.ktor.server.cio.CIOApplicationEngine")
public class CIOApplicationEngine_Instrumentation {

    @Trace(dispatcher = true)
    private Object handleRequest(ServerRequestScope serverRequestScope, Request request, Continuation<? super Unit> continuation) {
        Transaction transaction = NewRelic.getAgent().getTransaction();
        if (!transaction.isWebTransaction()) {
            transaction.convertToWebTransaction();
        }
        CIORequestHeaders cioHeaders = new CIORequestHeaders(request.getHeaders());
        transaction.acceptDistributedTraceHeaders(TransportType.HTTP, cioHeaders);
        String uri = request.getUri().toString();
        String method = request.getMethod().toString();
        String txName = uri.startsWith("/") ? uri.substring(1) : uri;
        if (txName.isEmpty()) txName = "Root";
        txName = txName + " - {" + method + "}";
        transaction.setTransactionName(TransactionNamePriority.CUSTOM_LOW, false, "KtorCIO", txName);
        return Weaver.callOriginal();
    }

}
