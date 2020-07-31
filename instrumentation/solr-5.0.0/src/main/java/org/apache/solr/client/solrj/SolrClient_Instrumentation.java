/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.client.solrj;

import com.agent.instrumentation.solr.SolrUtil;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
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
    public UpdateResponse add(Collection<SolrInputDocument> docs) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(SolrInputDocument doc) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse add(SolrInputDocument doc, int commitWithinMs) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "add");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse commit(boolean waitFlush, boolean waitSearcher) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "commit");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher, int maxSegments) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "optimize");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse rollback() {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "rollback");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteById(String id) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteById");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteById(String id, int commitWithinMs) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteById");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteById(List<String> ids) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteById");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteById(List<String> ids, int commitWithinMs) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteById");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteByQuery(String query) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteByQuery");
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateResponse deleteByQuery(String query, int commitWithinMs) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "deleteByQuery");
        return Weaver.callOriginal();
    }

    @Trace
    public SolrPingResponse ping() {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "ping");
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(SolrParams params) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "query");
        addQueryParams(params);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse query(SolrParams params, SolrRequest.METHOD method) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "query");
        addQueryParams(params);
        return Weaver.callOriginal();
    }

    @Trace
    public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback) {
        AgentBridge.getAgent().getTracedMethod().setMetricName("SolrClient", getClass().getName(), "queryAndStream");
        addQueryParams(params);
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