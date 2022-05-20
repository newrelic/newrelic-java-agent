package util;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;

import static util.TokenAndRefUtils.*;

public final class TokenAwareRunnable implements Runnable {
    private final Runnable delegate;

    private AgentBridge.TokenAndRefCount tokenAndRefCount;
    private Transaction transaction;

    public TokenAwareRunnable(Runnable delegate) {
        this.delegate = delegate;
        //get token state from calling Thread
        this.tokenAndRefCount = getThreadTokenAndRefCount();
        this.transaction = getTransaction(tokenAndRefCount);
        logTokenInfo(tokenAndRefCount, "TokenAwareRunnable token info set");
    }

    @Override
    public void run() {
        try {
            if (delegate != null) {
                logTokenInfo(tokenAndRefCount, "Token info set in thread");
                setThreadTokenAndRefCount(tokenAndRefCount, transaction);
                delegate.run();
            }
        } finally {
            logTokenInfo(tokenAndRefCount, "Clearing token info from thread ");
            clearThreadTokenAndRefCountAndTxn(tokenAndRefCount);
        }
    }
}
