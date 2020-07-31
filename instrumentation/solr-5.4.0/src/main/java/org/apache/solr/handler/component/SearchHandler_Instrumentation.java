/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.handler.component;

import com.agent.instrumentation.solr.SolrUtil;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.lucene.search.Query;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

import java.util.HashMap;
import java.util.Map;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.solr.handler.component.SearchHandler")
public abstract class SearchHandler_Instrumentation {

    @Trace
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) {
        Weaver.callOriginal();

        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (tx != null && tx.isStarted()) {
            Map<String, String> result = new HashMap<>();

            // I can't find a functional difference between these two from the original SolrRequestHandlerPointCut
            String solrQueryRequest = req.getParams().get("q");
            if (solrQueryRequest != null) {
                result.put(SolrUtil.SOLR_RAW_QUERY_STRING, solrQueryRequest);
                result.put(SolrUtil.SOLR_QUERY_STRING, solrQueryRequest);
            }
            // haven't found a replacement for this in Solr 4.0.0 yet
            Query query = SolrUtil.parseQuery(solrQueryRequest, null, req.getSchema());
            if (query != null && query.toString() != null) {
                String luceneQuery = query.toString();
                result.put(SolrUtil.SOLR_LUCENE_QUERY, luceneQuery);
                result.put(SolrUtil.SOLR_LUCENE_QUERY_STRING, luceneQuery);
            }

            if (rsp.getException() != null && rsp.getException().toString() != null) {
                result.put(SolrUtil.SOLR_DEBUG_INFO_ERROR, rsp.getException().toString());
            }

            tx.getAgentAttributes().putAll(result);
        }
    }

}
