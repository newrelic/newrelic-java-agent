# MongoDB Async Instrumentation

A `request` is made via the mongo reactivestreams client which invokes an operation on an `AsyncMongoIterableImpl`.
Each MongoDB operation invoked on `AsyncMongoIterableImpl` executes on a separate thread and asynchronously 
returns a result via the `onResult` method of `SingleResultCallback`.

```java
"reactor-http-nio-2@13695" daemon prio=5 tid=0x55 nid=NA runnable

    at com.nr.agent.mongo.NRCallbackWrapper.<init>(NRCallbackWrapper.java:22)
    at com.mongodb.internal.async.client.AsyncMongoIterableImpl.batchCursor(AsyncMongoIterableImpl.java:166)
    at com.mongodb.reactivestreams.client.internal.MongoIterableSubscription.requestInitialData(MongoIterableSubscription.java:45)
    ...
    at com.mongodb.reactivestreams.client.internal.AbstractSubscription.request(AbstractSubscription.java:100)
    ...
    at java.lang.Thread.run(Thread.java:829)
```

```java
"nioEventLoopGroup-3-3@14575" prio=10 tid=0x3a nid=NA runnable

    at com.nr.agent.mongo.NRCallbackWrapper.onResult(NRCallbackWrapper.java:34)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:48)
    at com.mongodb.internal.async.client.OperationExecutorImpl$1$1$1.onResult(OperationExecutorImpl.java:92)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:48)
    at com.mongodb.internal.operation.FindOperation$3.onResult(FindOperation.java:727)
    at com.mongodb.internal.operation.OperationHelper$ReferenceCountedReleasingWrappedCallback.onResult(OperationHelper.java:411)
    at com.mongodb.internal.operation.CommandOperationHelper$10.onResult(CommandOperationHelper.java:481)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:48)
    at com.mongodb.internal.connection.DefaultServer$DefaultServerProtocolExecutor$2.onResult(DefaultServer.java:251)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:48)
    at com.mongodb.internal.connection.CommandProtocolImpl$1.onResult(CommandProtocolImpl.java:84)
    at com.mongodb.internal.connection.DefaultConnectionPool$PooledConnection$2.onResult(DefaultConnectionPool.java:517)
    at com.mongodb.internal.connection.UsageTrackingInternalConnection$2.onResult(UsageTrackingInternalConnection.java:111)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:48)
    at com.mongodb.internal.connection.InternalStreamConnection$2$1.onResult(InternalStreamConnection.java:398)
    at com.mongodb.internal.connection.InternalStreamConnection$2$1.onResult(InternalStreamConnection.java:375)
    at com.mongodb.internal.connection.InternalStreamConnection$MessageHeaderCallback$MessageCallback.onResult(InternalStreamConnection.java:676)
    at com.mongodb.internal.connection.InternalStreamConnection$MessageHeaderCallback$MessageCallback.onResult(InternalStreamConnection.java:643)
    ...
    at java.lang.Thread.run(Thread.java:829)
```

This instrumentation weaves `AsyncMongoIterableImpl`, and all classes that extend it, to capture MongoDB query info
such as `collectionName`, `databaseName`, and `operationName`.

For each operation, the `SingleResultCallback` is wrapped with a `NRCallbackWrapper` which stores a `Token` for linking
the result thread to the originating thread, starts and stores a `Segment` for timing the external operation, and stores
`DatastoreParameters` detailing the external operation.

When the callback completes and invokes `onResult` the `NRCallbackWrapper` will link and expire the `Token` to tie the
result thread back to the originating thread, report the external datastore call, and end the `Segment` timing and then
delegate back to the wrapped `SingleResultCallback`.

`AsyncMongoCollectionImpl` is instrumented in the same manner for tracing operations asynchronously interacting with
a MongoDB collection. It will also generate metrics based on the operation (e.g. `Custom/AsyncMongoCollection/find`).


This instrumentation weaves `AsyncMongoDatabase` and all of its implementing classes to set the address.
