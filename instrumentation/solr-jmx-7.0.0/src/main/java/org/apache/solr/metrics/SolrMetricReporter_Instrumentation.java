/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.metrics;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.metrics.reporters.SolrJmxReporter;

import javax.management.MBeanServer;
import java.util.logging.Level;

@Weave(originalName = "org.apache.solr.metrics.SolrMetricReporter", type = MatchType.BaseClass)
public abstract class SolrMetricReporter_Instrumentation {

    public void init(PluginInfo pluginInfo) {
        Weaver.callOriginal();
        Logger logger = NewRelic.getAgent().getLogger();
        if (SolrJmxReporter.class.isInstance(this)) {
            Object thisTemp = this;
            SolrJmxReporter solrJMX = (SolrJmxReporter) thisTemp;
            MBeanServer mBeanServer = solrJMX.getMBeanServer();
            logger.log(Level.FINEST, "SolrJmxReporter mBeanServer: {0}", mBeanServer);
            if (mBeanServer != null) {
                AgentBridge.privateApi.addMBeanServer(mBeanServer);
                logger.log(Level.FINEST, "added mBeanServer: {0}", mBeanServer);
            }
        }
    }

}
