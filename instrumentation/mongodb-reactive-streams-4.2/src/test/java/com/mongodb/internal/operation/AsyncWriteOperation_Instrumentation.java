package com.mongodb.internal.operation;

import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.binding.AsyncWriteBinding;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "com.mongodb.internal.operation.AsyncWriteOperation")
public abstract class AsyncWriteOperation_Instrumentation<T> {


    void executeAsync(AsyncWriteBinding binding, SingleResultCallback<T> callback){

        Weaver.callOriginal();
    }
}
