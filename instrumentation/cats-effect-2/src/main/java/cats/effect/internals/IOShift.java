package cats.effect.internals;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.atomic.AtomicInteger;

@Weave(originalName = "cats.effect.internals.IOShift")
public class IOShift {

  @Weave(originalName = "cats.effect.internals.IOShift$Tick")
  public static class Tick {
    @NewField
    private AgentBridge.TokenAndRefCount tokenAndRefCount;

    public Tick(scala.Function1<scala.util.Either<java.lang.Throwable, scala.runtime.BoxedUnit>,
      scala.runtime.BoxedUnit> cb) {
        this.tokenAndRefCount = getThreadTokenAndRefCount();
      System.out.println(Thread.currentThread()+ "Tick Txn:"+ AgentBridge.getAgent().getTransaction(false));
      printTokenInfo(tokenAndRefCount, "Tick");
    }

    public void run() {
      try {
        setThreadTokenAndRefCount(this.tokenAndRefCount);
        printTokenInfo(tokenAndRefCount, "run");
        Weaver.callOriginal();
      } finally {
//        printTokenInfo(tokenAndRefCount, "clearing token from thread " + tokenAndRefCount.refCount.get());
        clearThreadTokenAndRefCountAndTxn(this.tokenAndRefCount);
      }
    }

    private static AgentBridge.TokenAndRefCount getThreadTokenAndRefCount() {
      AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
      if (tokenAndRefCount == null) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (tx != null) {
          tokenAndRefCount = new AgentBridge.TokenAndRefCount(tx.getToken(),
                                                              AgentBridge.getAgent().getTracedMethod(), new AtomicInteger(1));
        }
      } else {
//        printTokenInfo(tokenAndRefCount, "getting token from thread " + tokenAndRefCount);
        tokenAndRefCount.refCount.incrementAndGet();
      }
      return tokenAndRefCount;
    }

    private static void setThreadTokenAndRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
      if (tokenAndRefCount != null) {
        AgentBridge.activeToken.set(tokenAndRefCount);
        tokenAndRefCount.token.link();
      }
    }

    private static void clearThreadTokenAndRefCountAndTxn(AgentBridge.TokenAndRefCount tokenAndRefCount) {
      AgentBridge.activeToken.remove();
      if (tokenAndRefCount != null && tokenAndRefCount.refCount.decrementAndGet() == 0) {
        System.out.println(Thread.currentThread().getName() + "******** " + tokenAndRefCount.token + "::"+"expiring token");
//        printTokenInfo(tokenAndRefCount, "expiring token");
        tokenAndRefCount.token.expire();
        tokenAndRefCount.token = null;
      }
    }

    private static void printTokenInfo(AgentBridge.TokenAndRefCount tokenAndRefCount, String msg) {
      String tokenMsg = (tokenAndRefCount != null && tokenAndRefCount.token != null)
          ? String.format("[%s:%s]", tokenAndRefCount.token, tokenAndRefCount.token.getTransaction())
          : "[Empty token]";
      System.out.println(Thread.currentThread().getName() + "******** " + tokenMsg + "::"+msg);
    }
  }
}
