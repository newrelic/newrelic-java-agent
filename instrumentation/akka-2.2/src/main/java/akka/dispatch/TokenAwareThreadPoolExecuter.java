package akka.dispatch;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TokenAwareThreadPoolExecuter implements ExecutorService, LoadMetrics {

  private final ExecutorService delegate;

  public TokenAwareThreadPoolExecuter(ExecutorService threadPoolExecutor) {
    this.delegate = threadPoolExecutor;
  }
  @Override
  public void execute(Runnable runnable) {
    if (runnable != null) {
      runnable = new TokenAwareRunnable(runnable);
    }
    delegate.execute(runnable);
  }

  @Override
  public boolean atFullThrottle() {
    if(delegate instanceof LoadMetrics) {
      return ((LoadMetrics) delegate).atFullThrottle();
    }
    return false;
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(task);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate.submit(task, result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(task);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate.invokeAll(tasks);
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.invokeAll(tasks, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return delegate.invokeAny(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(tasks, timeout, unit);
  }
}
