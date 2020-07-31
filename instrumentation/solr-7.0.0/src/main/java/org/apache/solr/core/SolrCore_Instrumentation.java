/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ManifestUtils;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.solr.core.SolrCore")
public abstract class SolrCore_Instrumentation {

    @WeaveAllConstructors
    public SolrCore_Instrumentation() {
        AgentBridge.jmxApi.createMBeanServerIfNeeded();
        AgentBridge.jmxApi.addJmxMBeanGroup("solr7");
    }

    @Trace
    private void initListeners() {
        String version = ManifestUtils.getVersionFromManifest(getClass(), "Solr", "7.x");
        NewRelic.getAgent().getLogger().log(Level.FINE, "Detected Solr version = {0}", version);
        NewRelic.setServerInfo("Solr", version);
        Weaver.callOriginal();
    }

    @Trace
    public void execute(SolrRequestHandler handler, SolrQueryRequest req, SolrQueryResponse rsp) {
        Weaver.callOriginal();
    }

}
