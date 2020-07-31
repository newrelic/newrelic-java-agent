/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.core;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Level;

@Weave(type = MatchType.ExactClass, originalName = "org.apache.solr.core.SolrConfig")
public abstract class SolrConfig_Instrumentation {

    @Weave(type = MatchType.ExactClass, originalName = "org.apache.solr.core.SolrConfig$JmxConfiguration")
    public abstract static class JmxConfiguration_Instrumentation {

        public boolean enabled = Weaver.callOriginal();

        public JmxConfiguration_Instrumentation(boolean enabled, String agentId, String serviceUrl, String rootName) {
            if (!enabled) {
                enabled = true;
                AgentBridge.getAgent().getLogger().log(Level.FINE, "Enabling Solr JMX metrics");
            }
        }
    }

}
