package util;

import com.newrelic.agent.bridge.AgentBridge;

import static util.TokenAndRefUtils.clearThreadTokenAndRefCountAndTxn;
import static util.TokenAndRefUtils.getThreadTokenAndRefCount;
import static util.TokenAndRefUtils.logTokenInfo;
import static util.TokenAndRefUtils.setThreadTokenAndRefCount;

public final class TokenAwareRunnable implements Runnable {
    private final Runnable delegate;

    private AgentBridge.TokenAndRefCount tokenAndRefCount;

    public TokenAwareRunnable(Runnable delegate) {
        this.delegate = delegate;
        //get token state from calling Thread
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
