package cats.effect.internals;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import static cats.effect.internals.Utils.clearThreadTokenAndRefCountAndTxn;
import static cats.effect.internals.Utils.setThreadTokenAndRefCount;
import static cats.effect.internals.Utils.getThreadTokenAndRefCount;
import static cats.effect.internals.Utils.logTokenInfo;

@Weave(originalName = "cats.effect.internals.IOShift")
public class IOShift {

  @Weave(originalName = "cats.effect.internals.IOShift$Tick")
  public static class Tick {
    @NewField
    private AgentBridge.TokenAndRefCount tokenAndRefCount;

    public Tick(scala.Function1<scala.util.Either<java.lang.Throwable, scala.runtime.BoxedUnit>,
      scala.runtime.BoxedUnit> cb) {
      this.tokenAndRefCount = getThreadTokenAndRefCount();
      logTokenInfo(tokenAndRefCount, "IOTick token info set");
    }

    public void run() {
      try {
        setThreadTokenAndRefCount(this.tokenAndRefCount);
        logTokenInfo(tokenAndRefCount, "Token info set in thread");
        Weaver.callOriginal();
      } finally {
        logTokenInfo(tokenAndRefCount, "Clearing token info from thread ");
        clearThreadTokenAndRefCountAndTxn(this.tokenAndRefCount);
      }
    }
  }
}
