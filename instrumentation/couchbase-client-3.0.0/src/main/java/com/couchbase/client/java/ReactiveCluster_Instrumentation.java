package com.couchbase.client.java;

import java.util.function.Consumer;

import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.ReactiveQueryResult;
import com.couchbase.client.java.search.SearchOptions;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.ReactiveSearchResult;
import com.newrelic.agent.database.ParsedDatabaseStatement;
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

@Weave(originalName = "com.couchbase.client.java.ReactiveCluster")
public class ReactiveCluster_Instrumentation {

    @Trace
    public Mono<ReactiveQueryResult> query(String statement, QueryOptions options) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("query");
        Mono<ReactiveQueryResult> result = Weaver.callOriginal();
        ParsedDatabaseStatement stmt = Utils.parseSQL(statement);
        if(stmt != null) {
            String collection = stmt.getOperation();
            String operation = stmt.getModel();
            DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(collection).operation(operation).build();
            NRHolder holder = new NRHolder(segment, params);

            Consumer<SignalType> onFinally = new NRSignalConsumer(holder);
            Consumer<? super Throwable> onError = new NRErrorConsumer(holder);
            return result.doFinally(onFinally).doOnError(onError);
        } else if(segment != null) {
            segment.ignore();
            segment = null;
        }

        return result;
    }

    @Trace
    public Mono<ReactiveSearchResult> searchQuery(String indexName, SearchQuery query, SearchOptions options) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("searchQuery");
        Mono<ReactiveSearchResult> result = Weaver.callOriginal();

        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection("?").operation("searchQuery").build();

        NRHolder holder = new NRHolder(segment, params);

        return result.doFinally(new NRSignalConsumer(holder)).doOnError(new NRErrorConsumer(holder));
    }
}
