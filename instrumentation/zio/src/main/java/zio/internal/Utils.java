package zio.internal;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;

public class Utils {

  public static AgentBridge.TokenAndRefCount getThreadTokenAndRefCount() {
    AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
    Transaction tx = AgentBridge.getAgent().getTransaction(false);
    if (tx != null) {
      tokenAndRefCount = new AgentBridge.TokenAndRefCount(tx.getToken(),
                                                          AgentBridge.getAgent().getTracedMethod(), new AtomicInteger(1));
    }
    return tokenAndRefCount;
  }

  public static void setThreadTokenAndRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    if (tokenAndRefCount != null) {
      AgentBridge.activeToken.set(tokenAndRefCount);
      tokenAndRefCount.token.link();
    }
  }

  public static void clearThreadTokenAndRefCountAndTxn(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    AgentBridge.activeToken.remove();
    if (tokenAndRefCount != null) { //removed a call to decrement the ref count as it is no longer being used
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
