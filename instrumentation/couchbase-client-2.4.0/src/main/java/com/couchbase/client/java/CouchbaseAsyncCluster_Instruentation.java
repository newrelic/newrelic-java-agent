package com.couchbase.client.java;

import java.util.List;

import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.transcoder.Transcoder;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.couchbase.NRCouchbaseAction;

import rx.Observable;

@Weave(originalName = "com.couchbase.client.java.CouchbaseAsyncCluster")
public abstract class CouchbaseAsyncCluster_Instruentation {

    @SuppressWarnings("rawtypes")
    @Trace
    public Observable<AsyncBucket> openBucket(final String name, final String password,
            final List<Transcoder<? extends Document, ?>> transcoders) {
        
        Observable<AsyncBucket> bucket = Weaver.callOriginal();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("openBucket-"+name);
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(name).operation("openBucket").build();
        NRCouchbaseAction<Throwable> onCompleted = new NRCouchbaseAction<Throwable>(segment, params);
        return bucket.doOnCompleted(onCompleted).doOnError(onCompleted);
        
    }
}
