# java.completable-future-jdk8

This instrumentation weaves `java.util.concurrent.CompletableFuture` and `java.util.concurrent.CompletableFuture$Async` to
trace code execution across asynchronous boundaries.

## How it works

Some context on parallelism according to comments in the 
[CompletableFuture source](http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/util/concurrent/CompletableFuture.java#l77):

> * All <em>async</em> methods without an explicit Executor
> * argument are performed using the {@link ForkJoinPool#commonPool()}
> * (unless it does not support a parallelism level of at least two, in
> * which case, a new Thread is used). To simplify monitoring,
> * debugging, and tracking, all generated asynchronous tasks are
> * instances of the marker interface {@link
> * AsynchronousCompletionTask}.

When `CompletableFuture.execAsync(Executor e, Async r)` is invoked, it "starts the given async task using the given executor, 
unless the executor is `ForkJoinPool.commonPool` and it has been disabled, in which case starts a new thread." 

Instrumented code:

```java
    static void execAsync(Executor e, CompletableFuture_Instrumentation.Async r) {
      if (noParallelism(e)) {
        new Thread(new TokenAwareRunnable(r)).start();
      } else {
        Executor tde = useTokenDelegateExecutor(e);
        if (null != tde) {
          tde.execute(r);
        }
      }
    }
```

### Case 1: No Parallelism

If there is no parallelism this instrumentation will initialize a new `Thread` with a `TokenAwareRunnable` that wraps the `CompletableFuture$Async` argument
passed to `execAsync`. The `TokenAwareRunnable` uses `TokenAndRefUtils` to get a `TokenAndRefCount`, if one exists, for the current `Thread`. Otherwise, it
creates a new `TokenAndRefCount`.

The `TokenAndRefCount` stores a `Token` that can be used to link asynchronous `Threads` together and tracks the number of incoming references to the `Token`.
When `TokenAwareRunnable.run()` is invoked the stored `Token` is linked on the executing `Thread` and finally the `Token` is expired when `run()` completes,
allowing the `Transaction` to complete.

### Case 2: Parallelism

In this case a `TokenDelegateExecutor` is initialized and used to wrap the `Executor` argument that was passed to `execAsync`. When
`TokenDelegateExecutor.execute(Runnable runnable)` is invoked it will initialize and store a `TokenAwareRunnable` that wraps the `CompletableFuture$Async`
argument passed to `execAsync`. From this point on, the `TokenAwareRunnable` functions exactly as described in Case 1: No Parallelism.

## Logging

This instrumentation will produce entries such as the following when searching the logs for keywords `token info`:

```
2022-01-07T17:22:03,481-0800 [53655 270] com.newrelic FINEST: [Empty token]: token info TokenAwareRunnable token info set
2022-01-07T17:22:03,482-0800 [53655 270] com.newrelic FINEST: [Empty token]: token info Token info set in thread
2022-01-07T17:22:03,482-0800 [53655 270] com.newrelic FINEST: [Empty token]: token info Clearing token info from thread
```

## Testing

See the following functional tests: `newrelic-java-agent/functional_test/src/test/java/test/newrelic/test/agent/CompletableFutureTest.java`