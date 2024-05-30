package org.eclipse.jetty.server.handler;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jetty12.JettyRequest;
import com.nr.agent.instrumentation.jetty12.JettyResponse;
import com.nr.agent.instrumentation.jetty12.ServerHelper;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

@Weave(originalName = "org.eclipse.jetty.server.handler.ContextHandler")
public class ContextHandler_Instrumentation {

    @WeaveAllConstructors
    public ContextHandler_Instrumentation() {
        ServerHelper.contextHandlerFound();
    }

    @Trace(dispatcher = true)
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        Transaction txn = AgentBridge.getAgent().getTransaction(true);
        txn.setWebRequest(new JettyRequest(request));
        txn.setWebResponse(new JettyResponse(response));

        return Weaver.callOriginal();
    }
}