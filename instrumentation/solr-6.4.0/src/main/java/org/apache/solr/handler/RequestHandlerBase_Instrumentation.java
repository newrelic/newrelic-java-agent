/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.handler;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

@Weave(type = MatchType.BaseClass, originalName = "org.apache.solr.handler.RequestHandlerBase")
public abstract class RequestHandlerBase_Instrumentation {

    @Trace
    public void handleRequest(SolrQueryRequest req, SolrQueryResponse rsp) {
        Weaver.callOriginal();
    }

}
