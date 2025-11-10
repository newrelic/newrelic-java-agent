package com.couchbase.client.java;

import java.util.HashMap;

import com.couchbase.client.java.datastructures.MutationOptionBuilder;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.AsyncSearchQueryResult;
import com.couchbase.client.java.view.AsyncSpatialViewResult;
import com.couchbase.client.java.view.AsyncViewResult;
import com.couchbase.client.java.view.SpatialViewQuery;
import com.couchbase.client.java.view.ViewQuery;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.couchbase.NRCouchbaseAction;

import rx.Observable;

@Weave(originalName = "com.couchbase.client.java.CouchbaseAsyncBucket")
public abstract class CouchbaseAsyncBucket_Instrumentation {

    public abstract String name();
    
    @Trace
    public <D extends Document<?>> Observable<D> append(final D document) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", document.id());
        attributes.put("BucketName", name());
        attributes.put("Operation", "append");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Append-"+name());
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("append").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }

    @Trace
    public <D extends Document<?>> Observable<D> append(D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", document.id());
        attributes.put("BucketName", name());
        attributes.put("Operation", "append");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Append-"+name());
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("append").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }

    @Trace
    public Observable<Boolean> close() {
        HashMap<String, Object> attributes = new HashMap<String, Object>();        
        attributes.put("BucketName", name());
        attributes.put("Operation", "close");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Close-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("close").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }

    @Trace
    public Observable<JsonLongDocument> counter(final String id, final long delta, final long initial, final int expiry) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", id);
        attributes.put("BucketName", name());
        attributes.put("Delta", delta);
        attributes.put("Initial", initial);
        attributes.put("Expiry", expiry);
        attributes.put("Operation", "counter");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<JsonLongDocument> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Counter-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("counter").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }

    @Trace
    public Observable<JsonLongDocument> counter(String id, long delta, long initial, int expiry, final PersistTo persistTo, final ReplicateTo replicateTo) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", id);
        attributes.put("BucketName", name());
        attributes.put("Delta", delta);
        attributes.put("Initial", initial);
        attributes.put("Expiry", expiry);
        attributes.put("Operation", "counter");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<JsonLongDocument> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Counter-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("counter").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }

    @Trace
    public Observable<Boolean> exists(final String id) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", id);
        attributes.put("BucketName", name());
        attributes.put("Operation", "exists");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Exists-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("exists").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }

    @Trace
    public <D extends Document<?>> Observable<D> get(final String id, final Class<D> target) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", id);
        attributes.put("BucketName", name());
        attributes.put("Operation", "get");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Append-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("append").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }

    @Trace
    public <D extends Document<?>> Observable<D> getAndLock(final String id, final int lockTime, final Class<D> target) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", id);
        attributes.put("BucketName", name());
        attributes.put("LockTime", lockTime);
        attributes.put("Operation", "getAndLock");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("GetAndLock-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("getAndLock").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }

    @Trace
    public <D extends Document<?>> Observable<D> getAndTouch(final String id, final int expiry, final Class<D> target) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", id);
        attributes.put("BucketName", name());
        attributes.put("Expiry", expiry);
        attributes.put("Operation", "getAndTouch");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("GetAndTouch-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("getAndTouch").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> getFromReplica(final String id, final ReplicaMode type,final Class<D> target) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", id);
        attributes.put("BucketName", name());
        attributes.put("Operation", "getFromReplica");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("GetFromReplica-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("getFromReplica").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> insert(final D document) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", document.id());
        attributes.put("BucketName", name());
        attributes.put("Operation", "insert");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Insert-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("insert").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> insert(final D document, final PersistTo persistTo,final ReplicateTo replicateTo) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", document.id());
        attributes.put("BucketName", name());
        attributes.put("Operation", "insert");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Insert-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("insert").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<Boolean> listAppend(final String docId, final E element, final MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", docId);
        attributes.put("BucketName", name());
        attributes.put("Operation", "listAppend");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("ListAppend-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("listAppend").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<E> listGet(String docId, int index, Class<E> elementType) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", docId);
        attributes.put("BucketName", name());
        attributes.put("Index", index);
        attributes.put("Operation", "listGet");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<E> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("ListGet-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("listGet").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<Boolean> listPrepend(final String docId, final E element, final MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", docId);
        attributes.put("BucketName", name());
        attributes.put("Operation", "listPrepend");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("ListPrepend-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("listPrepend").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public Observable<Boolean> listRemove(final String docId, final int index, final MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", docId);
        attributes.put("BucketName", name());
        attributes.put("Index", index);
        attributes.put("Operation", "listRemove");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("ListRemove-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("listRemove").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<Boolean> listSet(String docId, int index, E element, MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", docId);
        attributes.put("BucketName", name());
        attributes.put("Index", index);
        attributes.put("Operation", "listSet");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("ListSet-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("listSet").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <V> Observable<Boolean> mapAdd(final String docId,final String key,final V value,final MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", docId);
        attributes.put("BucketName", name());
        attributes.put("Key", key);
        attributes.put("Operation", "mapAdd");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("MapAdd-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("mapAdd").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <V> Observable<V> mapGet(final String docId, final String key, Class<V> valueType) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", docId);
        attributes.put("BucketName", name());
        attributes.put("Key", key);
        attributes.put("Operation", "mapGet");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<V> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("MapGet-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("mapGet").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public Observable<Boolean> mapRemove(final String docId,final String key,final MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", docId);
        attributes.put("BucketName", name());
        attributes.put("Key", key);
        attributes.put("Operation", "mapRemove");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("MapRemove-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("mapRemove").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> prepend(final D document) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", document.id());
        attributes.put("BucketName", name());
        attributes.put("Operation", "prepend");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Prepend-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("prepend").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> prepend(D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketID", document.id());
        attributes.put("BucketName", name());
        attributes.put("Operation", "prepend");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Prepend-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("prepend").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public Observable<AsyncN1qlQueryResult> query(final N1qlQuery query) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "query");
        attributes.put("QueryType", "N1ql");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<AsyncN1qlQueryResult> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Query-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("query").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public Observable<AsyncSearchQueryResult> query(final SearchQuery query) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "query");
        attributes.put("QueryType", "Search");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<AsyncSearchQueryResult> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Query-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("query").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public Observable<AsyncSpatialViewResult> query(final SpatialViewQuery query) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "query");
        attributes.put("QueryType", "SpatialView");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<AsyncSpatialViewResult> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Query-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("query").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public Observable<AsyncViewResult> query(final ViewQuery query) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "query");
        attributes.put("QueryType", "View");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<AsyncViewResult> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Query-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("query").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<E> queuePop(String docId, Class<E> elementType, MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "queuePop");
        attributes.put("DocId", docId);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<E> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("QueuePop-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("queuePop").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<Boolean> queuePush(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "queuePush");
        attributes.put("DocId", docId);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("QueuePush-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("queuePush").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> remove(final D document) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "remove");
        attributes.put("DocId", document.id());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("remove-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("remove").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> remove(D document, final PersistTo persistTo, final ReplicateTo replicateTo) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "remove");
        attributes.put("DocId", document.id());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("remove-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("remove").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> replace(final D document) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "replace");
        attributes.put("DocId", document.id());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("replace-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("replace").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<Boolean> setAdd(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "setAdd");
        attributes.put("DocId", docId);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("SetAdd-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("setAdd").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<Boolean> setContains(final String docId, final E element) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "setContains");
        attributes.put("DocId", docId);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("SetContains-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("setContains").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <E> Observable<E> setRemove(String docId, E element, MutationOptionBuilder mutationOptionBuilder) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "setRemove");
        attributes.put("DocId", docId);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<E> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("setRemove-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("setRemove").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public Observable<Boolean> touch(final String id, final int expiry) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "touch");
        attributes.put("DocId", id);
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("MapAdd-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("mapAdd").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<Boolean> unlock(D document) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "unlock");
        attributes.put("DocId", document.id());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<Boolean> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Unlock-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("unlock").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> upsert(final D document) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "upsert");
        attributes.put("DocId", document.id());
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("upsert-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("upsert").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    @Trace
    public <D extends Document<?>> Observable<D> upsert(final D document, final PersistTo persistTo,final ReplicateTo replicateTo) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("BucketName", name());
        attributes.put("Operation", "upsert");
        attributes.put("DocId", document.id());
        Observable<D> result = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("upsert-"+name());
        NewRelic.getAgent().getTransaction().getToken();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name()).operation("upsert").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return result.doOnCompleted(onCompleted).doOnError(onCompleted);
    }
    
    
}
