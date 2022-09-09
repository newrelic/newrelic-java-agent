package cats.effect.internals;

import cats.effect.IOFiber;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
public final class Utils {
  private Utils() {
  }

  public static AgentBridge.TokenAndRefCount getThreadTokenAndRefCount(IOFiber<?> ioFiber) {
    AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
    if (tokenAndRefCount == null) {
      Transaction tx = AgentBridge.getAgent().getTransaction(false);
      if (tx != null) {
        tokenAndRefCount = new AgentBridge.TokenAndRefCount(tx.getToken(),
                                                            AgentBridge.getAgent().getTracedMethod(),
                                                            new AtomicInteger(0));
        AgentBridge.activeToken.set(tokenAndRefCount);
      }
    }
    logTokenInfo(tokenAndRefCount, ioFiber, "getThreadTokenAndRefCount");
    return tokenAndRefCount;
  }

  public static void incrementTokenRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    if (tokenAndRefCount != null) {
      if (tokenAndRefCount.refCount != null) {
        tokenAndRefCount.refCount.incrementAndGet();
      } else {
        tokenAndRefCount.refCount = new AtomicInteger(1);
      }
    }
  }

  public static void decrementTokenRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    if (tokenAndRefCount != null) {
      tokenAndRefCount.refCount.decrementAndGet();
    }
  }
  public static void attemptExpireTokenRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount, IOFiber<?> ioFiber) {
      if (tokenAndRefCount != null && tokenAndRefCount.refCount.get() == 0) {
        expireOrDecTokenRefToken(tokenAndRefCount);
        logTokenInfo(tokenAndRefCount, ioFiber, "token expired");
      } else if (tokenAndRefCount != null) {
        logTokenInfo(tokenAndRefCount, ioFiber, "unable to expire refcount"+ tokenAndRefCount.refCount.get());
      }
  }

  public static void setThreadTokenAndRefCount(AtomicReference<AgentBridge.TokenAndRefCount> tokenAndRefCount, IOFiber<?> ioFiber) {
    if (tokenAndRefCount.get() != null && tokenAndRefCount.get().token != null) {
      logTokenInfo(tokenAndRefCount.get(), ioFiber,"setting token to thread");
      AgentBridge.activeToken.set(tokenAndRefCount.get());
      tokenAndRefCount.get().token.link();
    }
  }


  public static void expireOrDecTokenRefToken(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    if (tokenAndRefCount != null) {
      tokenAndRefCount.token.expire();
      tokenAndRefCount.token = null;
      AgentBridge.activeToken.remove();
    }
  }

  public static void logTokenInfo(AgentBridge.TokenAndRefCount tokenAndRefCount,  IOFiber<?> ioFiber, String msg) {
    int fiberHash = ioFiber == null ? -1 : ioFiber.hashCode();
    if (AgentBridge.getAgent().getLogger().isLoggable(Level.FINEST)) {
      String tokenMsg = (tokenAndRefCount != null && tokenAndRefCount.token != null)
        ? String.format("[%s:%s:%d]", tokenAndRefCount.token, tokenAndRefCount.token.getTransaction(),
                        tokenAndRefCount.refCount.get())
        : "[Empty token]";
      AgentBridge.getAgent().getLogger().log(Level.FINEST, MessageFormat.format("{0}:{1} IOFiber[{2}] token info {3}",
                                                                                tokenMsg,
                                                                                NewRelic.getAgent().getTransaction(),
                                                                                fiberHash,
                                                                                msg));
    }
  }


}
