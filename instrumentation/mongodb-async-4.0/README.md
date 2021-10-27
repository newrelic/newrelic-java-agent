# MongoDB Async Instrumentation

A `request` is made via the mongo reactivestreams client which invokes an operation on an `AsyncMongoIterableImpl`.
Each MongoDB operation invoked on `AsyncMongoIterableImpl` executes on a separate thread and asynchronously 
returns a result via a `SingleResultCallback`.

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


This instrumentation weaves `AsyncMongoDatabase` and all of its implementing classes
to set the address
