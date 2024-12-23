package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class Utils {
    private Utils() {}

    public static AgentBridge.TokenAndRefCount getThreadTokenAndRefCount() {
        AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
        if (tokenAndRefCount == null) {
            Transaction txn = AgentBridge.getAgent().getTransaction(false);
            if (txn != null) {
                tokenAndRefCount = new AgentBridge.TokenAndRefCount(
                        txn.getToken(),
                        AgentBridge.getAgent().getTracedMethod(),
                        new AtomicInteger(1));
                AgentBridge.activeToken.set(tokenAndRefCount);
            }
        }
        logTokenInfo(tokenAndRefCount, "getThreadTokenAndRefCount");
        return tokenAndRefCount;
    }

    public static void setThreadTokenAndRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
        if (tokenAndRefCount != null && tokenAndRefCount.token != null) {
            logTokenInfo(tokenAndRefCount, "setThreadTokenAndRefCount");
            AgentBridge.activeToken.set(tokenAndRefCount);
            tokenAndRefCount.token.link();
        }
    }

    public static void clearThreadTokenAndRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
        AgentBridge.activeToken.remove();
        if (tokenAndRefCount != null && tokenAndRefCount.refCount.decrementAndGet() == 0) {
            if (tokenAndRefCount.token != null) {
                logTokenInfo(tokenAndRefCount, "clearThreadTokenAndRefCount");
                tokenAndRefCount.token.expire();
                tokenAndRefCount.token = null;
            }
        }
    }

    public static void logTokenInfo(AgentBridge.TokenAndRefCount tokenAndRefCount, String message) {
        if (AgentBridge.getAgent().getLogger().isLoggable(Level.FINEST)) {
            String tokenMessage = (tokenAndRefCount != null && tokenAndRefCount.token != null)
                    ? String.format("[%s:%s:%d]", tokenAndRefCount.token, tokenAndRefCount.token.getTransaction(), tokenAndRefCount.refCount.get())
                    : "[Empty token]";
            AgentBridge.getAgent().getLogger().log(Level.FINEST, String.format("%s: %s", message, tokenMessage));
        }
    }
}
