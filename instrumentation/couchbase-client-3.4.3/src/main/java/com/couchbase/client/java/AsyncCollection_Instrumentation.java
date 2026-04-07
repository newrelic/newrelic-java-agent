package com.couchbase.client.java;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

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
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.couchbase.NRBiConsumer;
import com.nr.instrumentation.couchbase.Utils;

@Weave(originalName = "com.couchbase.client.java.AsyncCollection")
public abstract class AsyncCollection_Instrumentation {

    public abstract String name();

    public abstract String bucketName();
    
    public abstract String scopeName();

    @Trace
    public CompletableFuture<ExistsResult> exists(String id, ExistsOptions options) {
        String operation = "exists";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<ExistsResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<ExistsResult, Throwable> action = new NRBiConsumer<ExistsResult>(segment, params);
        return result.whenComplete(action);
    }

    @Trace
    public CompletableFuture<GetResult> get(String id, GetOptions options) {
        String operation = "get";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Transaction transaction = NewRelic.getAgent().getTransaction();
        Segment segment = transaction.startSegment(operation);
        CompletableFuture<GetResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<GetResult, Throwable> action = new NRBiConsumer<GetResult>(segment, params);
        return result.whenComplete(action);
    }

    @Trace
    public CompletableFuture<List<CompletableFuture<GetReplicaResult>>> getAllReplicas(String id,GetAllReplicasOptions options) {
        String operation = "getAllReplicas";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<List<CompletableFuture<GetReplicaResult>>> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<List<CompletableFuture<GetReplicaResult>>, Throwable> action = new NRBiConsumer<List<CompletableFuture<GetReplicaResult>>>(segment, params);
        return result.whenComplete(action);
    }

    @Trace
    public CompletableFuture<GetResult> getAndLock(String id, Duration lockTime,GetAndLockOptions options) {
        String operation = "getAndLock";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<GetResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<GetResult, Throwable> action = new NRBiConsumer<GetResult>(segment, params);
        return result.whenComplete(action);
    }

    @Trace
    public CompletableFuture<GetResult> getAndTouch(String id, Duration expiry,GetAndTouchOptions options) {
        String operation = "getAndTouch";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<GetResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<GetResult, Throwable> action = new NRBiConsumer<GetResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<GetReplicaResult> getAnyReplica(String id, GetAnyReplicaOptions options) {
        String operation = "getAnyReplica";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<GetReplicaResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<GetReplicaResult, Throwable> action = new NRBiConsumer<GetReplicaResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<MutationResult> insert(String id, Object content, InsertOptions options) {
        String operation = "insert";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Transaction transaction = NewRelic.getAgent().getTransaction();
        Segment segment = transaction.startSegment(operation);
        CompletableFuture<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<MutationResult, Throwable> action = new NRBiConsumer<MutationResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<LookupInResult> lookupIn(String id, List<LookupInSpec> specs,LookupInOptions options) {
        String operation = "lookupIn";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<LookupInResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<LookupInResult, Throwable> action = new NRBiConsumer<LookupInResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<MutateInResult> mutateIn(String id,List<MutateInSpec> specs,MutateInOptions options) {
        String operation = "mutateIn";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<MutateInResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<MutateInResult, Throwable> action = new NRBiConsumer<MutateInResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<MutationResult> remove(String id, RemoveOptions options) {
        String operation = "remove";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<MutationResult, Throwable> action = new NRBiConsumer<MutationResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<MutationResult> replace(String id, Object content, ReplaceOptions options) {
        String operation = "replace";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<MutationResult, Throwable> action = new NRBiConsumer<MutationResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<MutationResult> touch(String id, Duration expiry, TouchOptions options) {
        String operation = "touch";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<MutationResult, Throwable> action = new NRBiConsumer<MutationResult>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<Void> unlock(String id, long cas, UnlockOptions options) {
        String operation = "unlock";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<Void> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<Void, Throwable> action = new NRBiConsumer<Void>(segment, params);
        return result.whenComplete(action);
    }
    
    @Trace
    public CompletableFuture<MutationResult> upsert(String id, Object content, UpsertOptions options) {
        String operation = "upsert";
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("CollectionName", name());
        attributes.put("BucketName", bucketName());
        attributes.put("Operation", operation);
        attributes.put("ScopeName", scopeName());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(operation);
        CompletableFuture<MutationResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(Utils.getName(this)).operation(operation).build();

        BiConsumer<MutationResult, Throwable> action = new NRBiConsumer<MutationResult>(segment, params);
        return result.whenComplete(action);
    }

}
