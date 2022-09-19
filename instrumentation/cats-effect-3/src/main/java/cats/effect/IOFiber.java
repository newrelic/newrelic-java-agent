package cats.effect;

import cats.effect.kernel.Outcome;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;


import static cats.effect.internals.Utils.*;

@Weave(originalName = "cats.effect.IOFiber")
public class IOFiber {

  @NewField
  public AgentBridge.TokenAndRefCount tokenAndRefCount;

  @WeaveAllConstructors
  public IOFiber() {
    this.tokenAndRefCount = getThreadTokenAndRefCount();
    incrementTokenRefCount(this.tokenAndRefCount);
    logTokenInfo(tokenAndRefCount, "IOFiber token info set");
  }

  public void run() {
    logTokenInfo(tokenAndRefCount, "Fiber run start");
    boolean fiberTokenSet = this.tokenAndRefCount != null;
    try {
      if (fiberTokenSet) {
        setThreadTokenAndRefCount(tokenAndRefCount);
      }
      logTokenInfo(tokenAndRefCount, "Token info set in thread");
      Weaver.callOriginal();
    } finally {
      AgentBridge.TokenAndRefCount threadTokenAndRefCount = getThreadTokenAndRefCount();
      if (!fiberTokenSet && threadTokenAndRefCount != null) {
        setFiberTokenAndRefCount(this);
        incrementTokenRefCount(tokenAndRefCount);
      }
      attemptExpireTokenRefCount(threadTokenAndRefCount);
      logTokenInfo(threadTokenAndRefCount, "Fiber run complete");
    }
  }

  private void done(final Outcome oc) {
    Weaver.callOriginal();
    if (tokenAndRefCount != null) {
      decrementTokenRefCount(tokenAndRefCount);
      attemptExpireTokenRefCount(tokenAndRefCount);
    }
  }
}
