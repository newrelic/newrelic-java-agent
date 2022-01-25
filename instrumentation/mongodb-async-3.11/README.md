# MongoDB Async Instrumentation

A `request` is made via the mongo reactivestreams client which invokes an operation on an `MongoIterableImpl`.
Each MongoDB operation invoked on `MongoIterableImpl` executes on a separate thread and asynchronously 
returns a result via the `onResult` method of `SingleResultCallback`.

```java
"reactor-http-nio-9@14616" daemon prio=5 tid=0x65 nid=NA runnable

    at com.nr.agent.mongo.NRCallbackWrapper.<init>(NRCallbackWrapper.java:22)
    at com.mongodb.async.client.MongoIterableImpl.batchCursor(MongoIterableImpl.java:166)
    at com.mongodb.async.client.MongoIterableSubscription.requestInitialData(MongoIterableSubscription.java:46)
    ...
    at com.mongodb.async.client.AbstractSubscription.request(AbstractSubscription.java:87)
    ...
    at java.lang.Thread.run(Thread.java:829)
```

```java
"nioEventLoopGroup-2-3@14930" prio=10 tid=0x55 nid=NA runnable

    at com.nr.agent.mongo.NRCallbackWrapper.onResult(NRCallbackWrapper.java:34)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:49)
    at com.mongodb.async.client.OperationExecutorImpl$1$1$1.onResult(OperationExecutorImpl.java:94)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:49)
    at com.mongodb.operation.FindOperation$3.onResult(FindOperation.java:827)
    at com.mongodb.operation.OperationHelper$ReferenceCountedReleasingWrappedCallback.onResult(OperationHelper.java:412)
    at com.mongodb.operation.CommandOperationHelper$10.onResult(CommandOperationHelper.java:481)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:49)
    at com.mongodb.internal.connection.DefaultServer$DefaultServerProtocolExecutor$2.onResult(DefaultServer.java:245)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:49)
    at com.mongodb.internal.connection.CommandProtocolImpl$1.onResult(CommandProtocolImpl.java:85)
    at com.mongodb.internal.connection.DefaultConnectionPool$PooledConnection$1.onResult(DefaultConnectionPool.java:467)
    at com.mongodb.internal.connection.UsageTrackingInternalConnection$2.onResult(UsageTrackingInternalConnection.java:111)
    at com.mongodb.internal.async.ErrorHandlingResultCallback.onResult(ErrorHandlingResultCallback.java:49)
    at com.mongodb.internal.connection.InternalStreamConnection$2$1.onResult(InternalStreamConnection.java:399)
    at com.mongodb.internal.connection.InternalStreamConnection$2$1.onResult(InternalStreamConnection.java:376)
    at com.mongodb.internal.connection.InternalStreamConnection$MessageHeaderCallback$MessageCallback.onResult(InternalStreamConnection.java:677)
    at com.mongodb.internal.connection.InternalStreamConnection$MessageHeaderCallback$MessageCallback.onResult(InternalStreamConnection.java:644)
    ...
    at java.lang.Thread.run(Thread.java:829)
```

This instrumentation weaves `MongoIterableImpl`, and all classes that extend it, to capture MongoDB query info
such as `collectionName`, `databaseName`, and `operationName`.

For each operation, the `SingleResultCallback` is wrapped with a `NRCallbackWrapper` which stores a `Token` for linking
the result thread to the originating thread, starts and stores a `Segment` for timing the external operation, and stores
`DatastoreParameters` detailing the external operation.

When the callback completes and invokes `onResult` the `NRCallbackWrapper` will link and expire the `Token` to tie the
result thread back to the originating thread, report the external datastore call, and end the `Segment` timing and then
delegate back to the wrapped `SingleResultCallback`.

`MongoCollectionImpl` is instrumented in the same manner for tracing operations asynchronously interacting with
a MongoDB collection. It will also generate metrics based on the operation (e.g. `Custom/MongoCollection/find`).


This instrumentation weaves `MongoDatabase` and all of its implementing classes to set the address.
