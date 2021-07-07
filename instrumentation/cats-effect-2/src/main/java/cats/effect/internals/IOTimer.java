package cats.effect.internals;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Function1;
import scala.concurrent.ExecutionContext;
import scala.runtime.BoxedUnit;
import scala.util.Either;

import static cats.effect.internals.Utils.clearThreadTokenAndRefCountAndTxn;
import static cats.effect.internals.Utils.setThreadTokenAndRefCount;
import static cats.effect.internals.Utils.getThreadTokenAndRefCount;
import static cats.effect.internals.Utils.logTokenInfo;

@Weave(originalName = "cats.effect.internals.IOTimer")
public class IOTimer {

  @Weave(originalName = "cats.effect.internals.IOTimer$ShiftTick")
  public static class ShiftTick {
    @NewField
    private AgentBridge.TokenAndRefCount tokenAndRefCount;

    public ShiftTick(final IOConnection conn, final Function1<Either<Throwable, BoxedUnit>, BoxedUnit> cb,
                     final ExecutionContext ec) {
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
