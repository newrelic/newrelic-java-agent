package com.nr.agent.instrumentation.jetty12;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class ServerHelper {

    private static final AtomicBoolean HAS_CONTEXT_HANDLER = new AtomicBoolean(false);

    /**
     * If there is a #ContextHandler, then Jetty is not embedded.
     *
     * @return true if there is a Jetty #ContextHandler.
     */
    public static boolean hasContextHandler() {
        return HAS_CONTEXT_HANDLER.get();
    }

    public static void contextHandlerFound() {
        if (!HAS_CONTEXT_HANDLER.getAndSet(true)) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, "Detected Jetty ContextHandler");
        }
    }
}