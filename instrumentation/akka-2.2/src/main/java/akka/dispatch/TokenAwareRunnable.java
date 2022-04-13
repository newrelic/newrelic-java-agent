package akka.dispatch;

import com.newrelic.agent.bridge.AgentBridge;

import static akka.dispatch.Utils.*;

public final class TokenAwareRunnable implements Runnable {
  private final Runnable delegate;
  private final AgentBridge.TokenAndRefCount tokenAndRefCount;

  public TokenAwareRunnable(Runnable delegate) {
    this.delegate = delegate;
    this.tokenAndRefCount = getThreadTokenAndRefCount();
    logTokenInfo(tokenAndRefCount, "TokenAwareRunnable token info set");
  }

  @Override
  public void run() {
    try {
      if (delegate != null) {
        logTokenInfo(tokenAndRefCount, "Token info set in thread");
        setThreadTokenAndRefCount(tokenAndRefCount);
        delegate.run();
      }
    } finally {
      logTokenInfo(tokenAndRefCount, "Clearing token info from thread ");
      clearThreadTokenAndRefCountAndTxn(tokenAndRefCount);
    }
  }
}