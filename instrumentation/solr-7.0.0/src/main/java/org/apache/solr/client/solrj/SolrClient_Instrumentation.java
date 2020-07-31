/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.client.solrj;

import com.agent.instrumentation.solr.SolrUtil;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Weave(type = MatchType.BaseClass, originalName = "org.apache.solr.client.solrj.SolrClient")
public abstract class SolrClient_Instrumentation {

    @Trace
    public UpdateResponse add(String collection, Collection<SolrInputDocument> docs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(Collection<SolrInputDocument> docs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(String collection, Collection<SolrInputDocument> docs, int commitWithinMs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(String collection, SolrInputDocument doc) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(SolrInputDocument doc) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(String collection, SolrInputDocument doc, int commitWithinMs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(SolrInputDocument doc, int commitWithinMs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(String collection, Iterator<SolrInputDocument> docIterator) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "commit");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher, boolean softCommit) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "commit");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher, int maxSegments) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "optimize");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse rollback(String collection) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "rollback");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteById(String collection, String id, int commitWithinMs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteById");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteById(String collection, List<String> ids, int commitWithinMs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteById");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteByQuery(String collection, String query, int commitWithinMs) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteByQuery");
        return Weaver.callOriginal();
    }

    @Trace
    public SolrPingResponse ping() {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "ping");
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(String collection, SolrParams params) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "query");
        addQueryParams(params);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(SolrParams params) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "query");
        addQueryParams(params);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(String collection, SolrParams params, SolrRequest.METHOD method) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "query");
        addQueryParams(params);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(SolrParams params, SolrRequest.METHOD method) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "query");
        addQueryParams(params);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse queryAndStreamResponse(String collection, SolrParams params, StreamingResponseCallback callback) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "queryAndStream");
        addQueryParams(params);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "queryAndStream");
        addQueryParams(params);
        return Weaver.callOriginal();
    }

    @Trace
    public SolrDocument getById(String collection, String id, SolrParams params) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "getById");
        return Weaver.callOriginal();
    }

    @Trace
    public SolrDocumentList getById(String collection, Collection<String> ids, SolrParams params) {
        NewRelic.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "getById");
        return Weaver.callOriginal();
    }

    private void addQueryParams(SolrParams solrParams) {
        Map<String, String[]> paramMap = new HashMap<>();
        Iterator<String> paramNamesIter = solrParams.getParameterNamesIterator();
        while (paramNamesIter.hasNext()) {
            String paramName = paramNamesIter.next();
            paramMap.put(paramName, solrParams.getParams(paramName));
        }

        if (!paramMap.isEmpty()) {
            AgentBridge.privateApi.addTracerParameter("query_params", SolrUtil.getSimpleParameterMap(paramMap, 255));
        }
    }

}