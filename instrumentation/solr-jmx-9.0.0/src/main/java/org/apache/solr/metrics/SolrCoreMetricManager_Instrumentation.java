/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.metrics;

import com.agent.instrumentation.solr.SolrComponentRegistry;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.core.SolrInfoBean;

@Weave(originalName = "org.apache.solr.metrics.SolrCoreMetricManager", type = MatchType.ExactClass)
public abstract class SolrCoreMetricManager_Instrumentation {

    public void registerMetricProducer(String scope, SolrMetricProducer producer) {
        // This creates the SolrMetricsContext instance
        Weaver.callOriginal();

        if (producer instanceof SolrInfoBean) {
            SolrInfoBean infoBean = (SolrInfoBean) producer;
            SolrMetricsContext context = producer.getSolrMetricsContext();

            if (context != null) {
                String tag = context.getTag();
                String name = infoBean.getName();
                SolrComponentRegistry.registerComponent(tag, name);
            }
        }
    }
}
