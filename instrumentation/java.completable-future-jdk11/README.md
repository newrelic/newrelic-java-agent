# java.completable-future-jdk11

This instrumentation weaves `java.util.concurrent.CompletableFuture` to trace code execution across asynchronous boundaries.

## JDK11 Updates

This instrumentation applies to JRE 11 and higher. This was necessary in order to add support for the method
`completeAsync` (introduced with Java 9).

JREs 8, 9 and 10 use the previous version of this module, `java.completable-future-jdk8u40`.

The instrumentation is otherwise the same as the `java.completable-future-jdk8u40`, and
works as described below. 

## How it works

When `CompletableFuture` methods (e.g. `uniApplyStage`, `biApplyStage`, `orApplyStage`, `asyncRunStage`, etc) are invoked, a `TokenDelegateExecutor`
is initialized and used to wrap the `Executor` argument that was passed to executing method. When `TokenDelegateExecutor.execute(Runnable runnable)` is
invoked it will initialize and store a `TokenAwareRunnable` that wraps the `Runnable` argument passed to `Executor`.

The `TokenAwareRunnable` uses `TokenAndRefUtils` to get a `TokenAndRefCount`, if one exists, for the  current `Thread`. Otherwise, it creates
a new `TokenAndRefCount`. The `TokenAndRefCount` stores a `Token` that can be used to link asynchronous `Threads` together and tracks the number of incoming references to the `Token`.
When `TokenAwareRunnable.run()` is invoked the stored `Token` is linked on the executing `Thread` and finally the `Token` is expired when `run()` completes,
allowing the `Transaction` to complete.

## Logging

This instrumentation will produce entries such as the following when searching the logs for keywords `token info`:

```
2022-01-07T17:22:03,481-0800 [53655 270] com.newrelic FINEST: [Empty token]: token info TokenAwareRunnable token info set
2022-01-07T17:22:03,482-0800 [53655 270] com.newrelic FINEST: [Empty token]: token info Token info set in thread
2022-01-07T17:22:03,482-0800 [53655 270] com.newrelic FINEST: [Empty token]: token info Clearing token info from thread
```

## Testing

Like all other modules instrumenting JDK classes, this module does not have instrumentation tests within the module
(this is a limitation of the introspector). Additionally, because this module is built with Java 11, it cannot otherwise
be tested in the functional tests. 

To work around these limitations, tests for this module have been placed in the `java-agent-integration-tests` project. 
Their content is similar to the existing CompletableFuture tests in 
`newrelic-java-agent/functional_test/src/test/java/test/newrelic/test/agent/CompletableFutureTest.java`.