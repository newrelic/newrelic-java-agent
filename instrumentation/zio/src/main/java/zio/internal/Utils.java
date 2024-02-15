package zio.internal;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import scala.Option;
import zio.Fiber;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class Utils {

  private static final ThreadLocal<HashMap<Fiber<?, ?>, AgentBridge.TokenAndRefCount>> mapStore =
          ThreadLocal.withInitial(() -> new HashMap<>());

  public static AgentBridge.TokenAndRefCount getThreadTokenAndRefCount() {
    AgentBridge.TokenAndRefCount tokenAndRefCount = getTokenAndRefCountFromFiber();
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

  public static void setThreadTokenAndRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    if (tokenAndRefCount != null) {
      setTokenAndRefCountFromFiber(tokenAndRefCount);
      tokenAndRefCount.token.link();
    }
  }

  public static void clearThreadTokenAndRefCountAndTxn(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    removeTokenAndRefCountFromFiber();
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

  private static AgentBridge.TokenAndRefCount getTokenAndRefCountFromFiber() {
    Fiber<?, ?> fiber = getCurrentFiber();
    HashMap<Fiber<?, ?>, AgentBridge.TokenAndRefCount> map = mapStore.get();
    return map.get(fiber);
  }

  private static void setTokenAndRefCountFromFiber(AgentBridge.TokenAndRefCount tokenAndRefCount) {
    Fiber<?, ?> fiber = getCurrentFiber();
    if (tokenAndRefCount == null) {
      return;
    }
    HashMap<Fiber<?, ?>, AgentBridge.TokenAndRefCount> map = mapStore.get();
    map.put(fiber, tokenAndRefCount);
  }

  private static void removeTokenAndRefCountFromFiber() {
    Fiber<?, ?> fiber = getCurrentFiber();
    HashMap<Fiber<?, ?>, AgentBridge.TokenAndRefCount> map = mapStore.get();
    map.remove(fiber);
  }

  private static Fiber<?, ?> getCurrentFiber() {
    Option<Fiber<Object, Object>> fiberOption = Fiber.unsafeCurrentFiber();
    if (fiberOption.isEmpty()) {
      return null;
    }
    return fiberOption.get();
  }


}
