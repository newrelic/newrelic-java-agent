package com.couchbase.client.java;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.search.SearchOptions;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.SearchResult;
import com.newrelic.agent.database.ParsedDatabaseStatement;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.couchbase.NRBiConsumer;
import com.nr.instrumentation.couchbase.Utils;

@Weave(originalName = "com.couchbase.client.java.AsyncCluster")
public abstract class AsyncCluster_Instrumentation {

    @Trace
    public CompletableFuture<QueryResult> query(String statement, QueryOptions options) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("Statement", statement);
        attributes.put("Operation", "query");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("query");
        CompletableFuture<QueryResult> result = Weaver.callOriginal();
        ParsedDatabaseStatement stmt = Utils.parseSQL(statement);
        if(stmt != null) {
            String collection = stmt.getOperation();
            String operation = stmt.getModel();
            DatastoreParameters params = DatastoreParameters.product("Couchbase").collection(collection).operation(operation).build();
            NRBiConsumer<QueryResult> consumer = new NRBiConsumer<QueryResult>(segment, params);
            return result.whenComplete(consumer);
        } else if(segment != null) {
            segment.ignore();
            segment = null;
        }
        
        return result;
    }
    
    @Trace
    public CompletableFuture<SearchResult> searchQuery(String indexName, SearchQuery query, SearchOptions options) {
        HashMap<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("IndexName", indexName);
        attributes.put("Operation", "searchQuery");
        NewRelic.getAgent().getTracedMethod().addCustomAttributes(attributes);
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("searchQuery");
        CompletableFuture<SearchResult> result = Weaver.callOriginal();
        DatastoreParameters params = DatastoreParameters.product("Couchbase").collection("?").operation("searchQuery").build();
        
        NRBiConsumer<SearchResult> consumer = new NRBiConsumer<SearchResult>(segment, params);
    
        return result.whenComplete(consumer);
    }
    
    
}
