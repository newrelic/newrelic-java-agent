# vertx-core-3.8.0

## FutureImpl#setHandler
Before version 3.8.4, this method only allowed a single handler to be stored, keeping only the last one provided. On version 3.8.4 and newer, multiple can be stored in an instance of Handlers.

The instrumentation for this method uses the same code as newer versions of Vert.x. And it works for both version to avoid dangling tokens. From 3.8.0 to 3.8.3, the instrumentation will create and expire tokens whenever a new Handler is set. For 3.8.4 it will at most create two Tokens.

This is the relevant code.
```java

    private Handler<AsyncResult> handler = Weaver.callOriginal();

    public Future setHandler(Handler<AsyncResult> handler) {
        Handler previousHandler = this.handler;
        // ,,,
        Future future = Weaver.callOriginal();

        if (!isComplete()) {
            if (previousHandler != this.handler) {
                VertxCoreUtil.storeToken(this.handler);
                if (previousHandler != null) {
                    VertxCoreUtil.expireToken(previousHandler);
                }
            }
        }
        return future;
    }
```

On 3.8.0, when this method is first called, the original code will set the provided Handler in `this.handler`. The instrumentation will store the token for that handler.

On the second call, the provided handler will be set in `this.handler`, a Token will be created for it. Since the first handler won't be executed, the Token for it will be expired.

The same will happen for any subsequent call.


Starting on 3.8.4, when this method is first called, the original code will set the provided Handler in `this.handler`. The instrumentation will store the token for that handler.

On the second call, a Handlers instance will be created, set to `this.handler` and both provided Handlers will be put in the Handlers. At this point the instrumentation will store the token for the Handlers instance and expire the token for the first Handler.

In subsequent calls, `this.handler` will be the Handlers instance throughout the execution of the method. So no further tokens will be created.
