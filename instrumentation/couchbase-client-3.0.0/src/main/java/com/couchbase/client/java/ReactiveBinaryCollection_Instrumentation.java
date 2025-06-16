package com.couchbase.client.java;

import java.util.HashMap;
import java.util.function.Consumer;

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
import com.nr.instrumentation.couchbase.NRErrorConsumer;
import com.nr.instrumentation.couchbase.NRHolder;
import com.nr.instrumentation.couchbase.NRSignalConsumer;
import com.nr.instrumentation.couchbase.Utils;

import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Weave(originalName = "com.couchbase.client.java.ReactiveBinaryCollection")
public abstract class ReactiveBinaryCollection_Instrumentation {
    
    private final AsyncBinaryCollection_Instrumentation async = Weaver.callOriginal();

    @Trace
    public Mono<MutationResult> append(String id, byte[] content, AppendOptions options) {
        String operation = "append";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        String name = Utils.getName(async.collectionIdentifier());
        attributes.put("CollectionName", name);
        attributes.put("Operation", operation);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<MutationResult> result = Weaver.callOriginal();
        CollectionIdentifier identifier = async.collectionIdentifier();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(identifier)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<CounterResult> decrement(String id, DecrementOptions options) {
        String operation = "decrement";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        String name = Utils.getName(async.collectionIdentifier());
        attributes.put("CollectionName", name);
        attributes.put("Operation", operation);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<CounterResult> result = Weaver.callOriginal();
        CollectionIdentifier identifier = async.collectionIdentifier();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(identifier)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<CounterResult> increment(String id, IncrementOptions options) {
        String operation = "increment";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        String name = Utils.getName(async.collectionIdentifier());
        attributes.put("CollectionName", name);
        attributes.put("Operation", operation);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<CounterResult> result = Weaver.callOriginal();
        CollectionIdentifier identifier = async.collectionIdentifier();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(identifier)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<MutationResult> prepend(String id, byte[] content, PrependOptions options) {
        String operation = "prepend";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        String name = Utils.getName(async.collectionIdentifier());
        attributes.put("CollectionName", name);
        attributes.put("Operation", operation);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<MutationResult> result = Weaver.callOriginal();
        CollectionIdentifier identifier = async.collectionIdentifier();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(identifier)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    
}
