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