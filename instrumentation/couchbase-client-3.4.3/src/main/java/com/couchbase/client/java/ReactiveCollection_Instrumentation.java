package com.couchbase.client.java;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import com.couchbase.client.java.kv.ExistsOptions;
import com.couchbase.client.java.kv.ExistsResult;
import com.couchbase.client.java.kv.GetAllReplicasOptions;
import com.couchbase.client.java.kv.GetAndLockOptions;
import com.couchbase.client.java.kv.GetAndTouchOptions;
import com.couchbase.client.java.kv.GetAnyReplicaOptions;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.kv.GetReplicaResult;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.InsertOptions;
import com.couchbase.client.java.kv.LookupInOptions;
import com.couchbase.client.java.kv.LookupInResult;
import com.couchbase.client.java.kv.LookupInSpec;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInResult;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.kv.RemoveOptions;
import com.couchbase.client.java.kv.ReplaceOptions;
import com.couchbase.client.java.kv.TouchOptions;
import com.couchbase.client.java.kv.UnlockOptions;
import com.couchbase.client.java.kv.UpsertOptions;
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Weave(originalName = "com.couchbase.client.java.ReactiveCollection")
public class ReactiveCollection_Instrumentation {
    
    private final AsyncCollection_Instrumentation asyncCollection = Weaver.callOriginal();
    
    @Trace
    public Mono<ExistsResult> exists(String id, ExistsOptions options) {
        String operation = "exists";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<ExistsResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<GetResult> get(String id, GetOptions options) {
        String operation = "get";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<GetResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Flux<GetReplicaResult> getAllReplicas(String id, GetAllReplicasOptions options) {
        String operation = "getAllReplicas";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Flux<GetReplicaResult>  result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<GetResult> getAndLock(String id, Duration lockTime, GetAndLockOptions options) {
        String operation = "getAndLock";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<GetResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<GetResult> getAndTouch(String id, Duration expiry, GetAndTouchOptions options) {
        String operation = "getAndTouch";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<GetResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<GetReplicaResult> getAnyReplica(String id, GetAnyReplicaOptions options) {
        String operation = "getAnyReplicas";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<GetReplicaResult>  result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<MutationResult> insert(String id, Object content, InsertOptions options) {
        String operation = "insert";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<LookupInResult> lookupIn(String id, List<LookupInSpec> specs, LookupInOptions options) {
        String operation = "lookupIn";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<LookupInResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<MutateInResult> mutateIn(String id, List<MutateInSpec> specs,MutateInOptions options) {
        String operation = "mutateIn";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<MutateInResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<MutationResult> remove(String id, RemoveOptions options) {
        String operation = "remove";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<MutationResult> replace(String id, Object content, ReplaceOptions options) {
        String operation = "replace";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<MutationResult> touch(String id, Duration expiry, TouchOptions options) {
        String operation = "touch";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<Void> unlock(String id, long cas, UnlockOptions options) {
        String operation = "unlock";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<Void> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
    @Trace
    public Mono<MutationResult> upsert(String id, Object content, UpsertOptions options) {
        String operation = "upsert";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", asyncCollection.name());
        attributes.put("BucketName", asyncCollection.bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", asyncCollection.scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        Mono<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(asyncCollection)).operation(operation).build();        
        NRHolder holder = new NRHolder(segment, params);
        Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
        Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
        return result.doFinally(onFinally ).doOnError(onError );
    }
    
}
