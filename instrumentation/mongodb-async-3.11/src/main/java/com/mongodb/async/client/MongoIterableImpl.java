package com.mongodb.async.client;

import java.util.Collection;

import com.mongodb.Block;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.async.AsyncBatchCursor;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.lang.Nullable;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.NRCallbackWrapper;

@Weave(type=MatchType.BaseClass)
abstract class MongoIterableImpl<TResult> {
	
	@NewField
	protected String collectionName = "Unknown";
	
	@NewField
	protected String databaseName = "Unknown";
	
	@NewField
	protected String operationName = "Unknown";
	
	MongoIterableImpl(@Nullable final ClientSession clientSession, final OperationExecutor executor, final ReadConcern readConcern,
            final ReadPreference readPreference, final boolean retryReads) {
		
	}

	@Trace
    public void first(SingleResultCallback<TResult> callback) {
    	
    	if(!(callback instanceof NRCallbackWrapper)) {
    		NRCallbackWrapper<TResult> wrapper = new NRCallbackWrapper<TResult>(callback);
    		wrapper.token = NewRelic.getAgent().getTransaction().getToken();
    		wrapper.params = databaseName != null ? DatastoreParameters.product(DatastoreVendor.MongoDB.name())
    				.collection(collectionName)
    				.operation(operationName)
    				.noInstance()
    				.databaseName(databaseName)
    				.build() :
    			DatastoreParameters.product(DatastoreVendor.MongoDB.name())
        			.collection(collectionName)
        			.operation(operationName)
        			.build();
    		wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
    		callback = wrapper;
    	}
    	Weaver.callOriginal();
    }

    @Trace
    public void forEach(Block<? super TResult> block, SingleResultCallback<Void> callback) {
    	if(!(callback instanceof NRCallbackWrapper)) {
    		NRCallbackWrapper<Void> wrapper = new NRCallbackWrapper<Void>(callback);
    		wrapper.token = NewRelic.getAgent().getTransaction().getToken();
    		wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
    				.collection(collectionName)
    				.operation(operationName)
    				.noInstance()
    				.databaseName(databaseName)
    				.build();
    		wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
    		callback = wrapper;
    	}
    	Weaver.callOriginal();
    }

    @Trace
    public <A extends Collection<? super TResult>> void into(A target, SingleResultCallback<A> callback) {
    	if(!(callback instanceof NRCallbackWrapper)) {
    		NRCallbackWrapper<A> wrapper = new NRCallbackWrapper<A>(callback);
    		wrapper.token = NewRelic.getAgent().getTransaction().getToken();
    		wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
    				.collection(collectionName)
    				.operation(operationName)
    				.noInstance()
    				.databaseName(databaseName)
    				.build();
    		wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
    		callback = wrapper;
    	}
    	Weaver.callOriginal();
    }

    @Trace
    public void batchCursor(SingleResultCallback<AsyncBatchCursor<TResult>> callback) {
    	if(!(callback instanceof NRCallbackWrapper)) {
    		NRCallbackWrapper<AsyncBatchCursor<TResult>> wrapper = new NRCallbackWrapper<AsyncBatchCursor<TResult>>(callback);
    		wrapper.token = NewRelic.getAgent().getTransaction().getToken();
    		wrapper.params = DatastoreParameters.product(DatastoreVendor.MongoDB.name())
    				.collection(collectionName)
    				.operation(operationName)
    				.noInstance()
    				.databaseName(databaseName)
    				.build();
    		wrapper.segment = NewRelic.getAgent().getTransaction().startSegment(operationName);
    		callback = wrapper;
    	}
    	Weaver.callOriginal();
    }
	
}
