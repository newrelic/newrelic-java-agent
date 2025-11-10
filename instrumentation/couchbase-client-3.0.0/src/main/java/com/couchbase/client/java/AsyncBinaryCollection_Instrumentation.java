package com.couchbase.client.java;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.couchbase.client.core.io.CollectionIdentifier;
import com.couchbase.client.java.kv.AppendOptions;
import com.couchbase.client.java.kv.CounterResult;
import com.couchbase.client.java.kv.DecrementOptions;
import com.couchbase.client.java.kv.IncrementOptions;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.kv.PrependOptions;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.couchbase.NRBiConsumer;
import com.nr.instrumentation.couchbase.Utils;

@Weave(originalName = "com.couchbase.client.java.AsyncBinaryCollection")
public abstract class AsyncBinaryCollection_Instrumentation {

    
    abstract CollectionIdentifier collectionIdentifier();
    
    @Trace
    public CompletableFuture<MutationResult> append(String id, byte[] content, AppendOptions options) {
        String operation = "append";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        String name = Utils.getName(collectionIdentifier());
        attributes.put("Name", name);
        attributes.put("Operation", operation);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(collectionIdentifier())).operation(operation).build();
        
        BiConsumer<MutationResult, Throwable> action = new NRBiConsumer<MutationResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<CounterResult> decrement(String id, DecrementOptions options) {
        String operation = "decrement";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        String name = Utils.getName(collectionIdentifier());
        attributes.put("Name", name);
        attributes.put("Operation", operation);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<CounterResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(collectionIdentifier())).operation(operation).build();
        
        BiConsumer<CounterResult, Throwable> action = new NRBiConsumer<CounterResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<CounterResult> increment(String id, IncrementOptions options) {
        String operation = "increment";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        String name = Utils.getName(collectionIdentifier());
        attributes.put("Name", name);
        attributes.put("Operation", operation);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<CounterResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(collectionIdentifier())).operation(operation).build();
        
        BiConsumer<CounterResult, Throwable> action = new NRBiConsumer<CounterResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<MutationResult> prepend(String id, byte[] content, PrependOptions options) {
        String operation = "prepend";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        String name = Utils.getName(collectionIdentifier());
        attributes.put("Name", name);
        attributes.put("Operation", operation);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(collectionIdentifier())).operation(operation).build();
        
        BiConsumer<MutationResult, Throwable> action = new NRBiConsumer<MutationResult>(segment, params);
        return result.whenComplete(action);
    }
    
}
