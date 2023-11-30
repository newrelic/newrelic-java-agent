# Async Transactions With Jetty

This describes the basic behavior of an async request made using the Jetty Continuations or
Servlet async API with Jetty as the dispatcher

## Request Flow

The basic flow of an async request using continuations/async servlet API and being handled by Jetty is something like this:

1. Jetty server receives a request and its `org.eclipse.jetty.server.Server.handle` method is invoked. 
2. Our Jetty instrumentation weaves `org.eclipse.jetty.server.Server.handle` and calls into our `ServerHelper.preHandle` method where we start a `Transaction`, set the dispatcher, record the request initialized, create tracers, etc. 
3. `org.eclipse.jetty.server.Request.startAsync` is then called by Jetty, which is another class that we instrument. This is where we register our `AsyncListener` with the `AsyncContext`. In Jetty the `AsyncContext` is an instance of `org.eclipse.jetty.server.AsyncContextState`, this is what allows us to manage transaction state across threads.
4. Execution returns back to `org.eclipse.jetty.server.Server.handle` and calls into our `ServerHelper.postHandle` method. If an async request was started we call `suspendAsync` on our `AsyncApiImpl`. We create an instance of `ServletAsyncTransactionStateImpl` and store it on the transaction. This is used to handle Servlet 3.0 and Jetty Continuations asynchronous processing state changes and manage the transaction instance across threads. We set the state to `SUSPENDING` and store the `Transaction` in the `asyncTransactions` map on `AsyncApiImpl`. We record the request as destroyed. 
5. ------ The thread is released and eventually the async work is dispatched to a new Jetty thread ------ 
6. Our `ServerHelper.preHandle` method is hit on the new thread, but this time it detects that the request is from `DispatcherType.ASYNC` and instead of creating a `Transaction` we call `resumeAsync` on our `AsyncApiImpl`. We get the suspended transaction from the `asyncTransactions` map and resume it, setting it as the current transaction on the new thread (storing it in the `ThreadLocal`) and changing the state from `SUSPENDING` to `RESUMING`. We verify that the transaction is active and if successful we change the state from `RESUMING` to `RUNNING`. We wrap the `Transaction` as a `BoundTransactionApiImpl` for reasons related to legacy async instrumentation. 
7. We hit our `ServerHelper.postHandle` method again, this time on the new thread. We call `suspendAsync` on our `AsyncApiImpl` again but this time it doesn't really do much of anything as we already have an instance of `ServletAsyncTransactionStateImpl`. The state changes from `RUNNING` to `SUSPENDING` but it doesn't seem to actually matter at this point though. The transaction is already stored in the `asyncTransactions` map, so it doesn't get added again, even though we try. 
8. At this point, we don’t have any way to know what the result on the async work was until the `AsyncListener` we register gets notified via one of the `onComplete`, `onError`, or `onTimeout` methods. 
   * If `onComplete` is called on the `AsyncListener` it will result in a call to `completeAsync` on our `AsyncApiImpl`. This will remove the transaction from the `asyncTransactions` map and complete the transaction. 
   * If `onError` is called on the `AsyncListener` it will result in a call to `errorAsync` on our `AsyncApiImpl`. This sets the `Throwable` on the `Transaction`. It doesn't seem to clear the stored transaction from the map on `AsyncApiImpl` though. 
   * If `onTimeout` is called on the `AsyncListener` it will result in a NoOp as we take no action.

## Known Issues

### Memory Leak

When a transaction is suspended by the legacy async API (i.e. `AsyncApiImpl`), the `AsyncContext` instance from the current request is used as a key to map to the `Transaction` instance that is being suspended (i.e. `asyncTransactions.put(asyncContext, currentTxn)`). When the request is eventually resumed, the `AsyncContext` instance can then be used to retrieve the suspended `Transaction` instance so that it can be resumed on whatever thread the async work is executing on.

In some rare cases, it is possible for a single request to result in the agent creating multiple `Transaction` instances and trying to suspend more than one of them (this has been observed with a combination of Jetty/Karaf/Camel CXF which caused Jetty `ContextHandler.doHandle` to be invoked multiple times, creating multiple transactions, and trying to suspend them all). The problem in this scenario, is that since multiple transactions are associated with a single request they all are associated with the same `AsyncContext` instance, which means that when those transactions are suspended multiple calls to `asyncTransactions.put(asyncContext, currentTxn)` will overwrite the `currentTxn` mapped to the `asyncContext`. Effectively, a transaction could be suspended and overwritten in the map, resulting it in never being resumed/completed, creating a memory leak of suspended transactions.

When analyzing a heap dump the issue would manifest as the `TransactionService` `updateQueue` retaining memory due to suspended transactions that never complete. Though to be clear, there are many reasons why the `updateQueue` could be holding onto transactions (long-running transactions, segment/token timeout issues, issues with other instrumentation) so further analysis must be done on what transactions are in the queue to determine the cause.

A workaround has been put in place to prevent this specific memory leak condition from being possible. It applies to the legacy async API itself, rather than being Jetty specific, which means that it could address such an issue with a wide range of app servers that are instrumented. To enable the workaround, set the following config to a value of `true` (default is `false`):

Config Option 1: Agent config file (this will update dynamically if the config file is changed)

```yaml
common: &default_settings
  legacy_async_api_skip_suspend: true
```

Config Option 2: System Property

```properties
-Dnewrelic.config.legacy_async_api_skip_suspend=true
```

Config Option 3: Environment Variable

```properties
NEW_RELIC_LEGACY_ASYNC_API_SKIP_SUSPEND=true
```
