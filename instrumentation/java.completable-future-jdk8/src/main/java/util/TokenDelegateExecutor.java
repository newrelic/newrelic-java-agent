package util;

import java.util.concurrent.Executor;

public class TokenDelegateExecutor implements Executor {
    public final Executor delegate;

    public TokenDelegateExecutor(final Executor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable runnable) {
        runnable = new TokenAwareRunnable(runnable);
        delegate.execute(runnable);
    }
}
