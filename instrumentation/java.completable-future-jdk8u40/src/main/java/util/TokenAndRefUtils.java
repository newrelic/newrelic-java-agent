package util;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class TokenAndRefUtils {

    public static AgentBridge.TokenAndRefCount getThreadTokenAndRefCount() {
        AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
        if (tokenAndRefCount == null) {
            Transaction tx = AgentBridge.getAgent().getTransaction(false);
            if (tx != null) {
                tokenAndRefCount = new AgentBridge.TokenAndRefCount(tx.getToken(),
                        AgentBridge.getAgent().getTracedMethod(), new AtomicInteger(1));
            }
        } else {
            tokenAndRefCount.refCount.incrementAndGet();
        }
        return tokenAndRefCount;
    }

    public static Transaction getTransaction(AgentBridge.TokenAndRefCount tokenAndRefCount) {
      if(tokenAndRefCount != null && tokenAndRefCount.token != null) {
        return (Transaction) tokenAndRefCount.token.getTransaction();
      } else {
        return null;
      }
    }

    public static void setThreadTokenAndRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount, Transaction transaction) {
        if (tokenAndRefCount != null && tokenAndRefCount.token != null) {
            AgentBridge.activeToken.set(tokenAndRefCount);
            tokenAndRefCount.token.link();
        } else if(tokenAndRefCount != null && transaction != null) {
          tokenAndRefCount.token = transaction.getToken();
          tokenAndRefCount.token.link();
          tokenAndRefCount.refCount = new AtomicInteger(1);
        }
    }

    public static void clearThreadTokenAndRefCountAndTxn(AgentBridge.TokenAndRefCount tokenAndRefCount) {
        AgentBridge.activeToken.remove();
        if (tokenAndRefCount != null && tokenAndRefCount.refCount.decrementAndGet() == 0) {
            tokenAndRefCount.token.expire();
            tokenAndRefCount.token = null;
        }
    }

    public static void logTokenInfo(AgentBridge.TokenAndRefCount tokenAndRefCount, String msg) {
        if (AgentBridge.getAgent().getLogger().isLoggable(Level.FINEST)) {
            String tokenMsg = (tokenAndRefCount != null && tokenAndRefCount.token != null)
                    ? String.format("[%s:%s:%d]", tokenAndRefCount.token, tokenAndRefCount.token.getTransaction(),
                    tokenAndRefCount.refCount.get())
                    : "[Empty token]";
            AgentBridge.getAgent().getLogger().log(Level.FINEST, MessageFormat.format("{0}: token info {1}", tokenMsg, msg));
        }
    }

}
