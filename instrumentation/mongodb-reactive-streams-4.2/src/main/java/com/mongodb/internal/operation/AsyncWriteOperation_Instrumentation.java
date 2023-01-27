package com.mongodb.internal.operation;

import com.mongodb.internal.async.SingleResultCallback;
import com.mongodb.internal.binding.AsyncWriteBinding;
import com.mongodb.internal.binding.WriteBinding;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.NRCallbackWrapper;
import org.bson.BsonDocument;
import org.bson.codecs.Decoder;

import static com.mongodb.assertions.Assertions.notNull;
import static com.mongodb.internal.operation.CommandOperationHelper.executeCommand;
import static com.mongodb.internal.operation.CommandOperationHelper.executeCommandAsync;

@Weave(type = MatchType.Interface, originalName = "com.mongodb.internal.operation.AsyncWriteOperation")
public abstract class AsyncWriteOperation_Instrumentation<T> {
    private final String databaseName = Weaver.callOriginal();
    private final BsonDocument command = Weaver.callOriginal();
    private final Decoder<T> decoder = Weaver.callOriginal();



    public void executeAsync(final AsyncWriteBinding binding, final SingleResultCallback<T> callback) {


        Weaver.callOriginal();
        executeCommandAsync(binding, databaseName, command, decoder, callback);
    }

    public T execute(WriteBinding binding) {
        return Weaver.callOriginal();
    }


    private <T> SingleResultCallback<T> instrument(SingleResultCallback<T> callback, String operationName) {
        if (callback instanceof NRCallbackWrapper) {
            return callback;
        }

        NRCallbackWrapper<T> wrapper = new NRCallbackWrapper<T>(callback);
        wrapper.params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection("collection-holder")
                .operation(operationName)
                .noInstance()
//                .instance(address.getHost(), address.getPort())
                .databaseName(databaseName)
                .build();

        wrapper.token = NewRelic.getAgent().getTransaction().getToken();
        wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
        return wrapper;
    }
}




