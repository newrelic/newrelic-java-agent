/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.JmxApi;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.values.EmbeddedTomcatDataSourceJmxValues;
import com.newrelic.agent.jmx.values.EmbeddedTomcatJmxValues;
import com.newrelic.agent.jmx.values.GlassfishJmxValues;
import com.newrelic.agent.jmx.values.Jboss7UpJmxValues;
import com.newrelic.agent.jmx.values.JettyJmxMetrics;
import com.newrelic.agent.jmx.values.KafkaConsumerJmxValues;
import com.newrelic.agent.jmx.values.KafkaProducerJmxValues;
import com.newrelic.agent.jmx.values.NonIteratedSolr7JmxValues;
import com.newrelic.agent.jmx.values.ResinJmxValues;
import com.newrelic.agent.jmx.values.Solr7JmxValues;
import com.newrelic.agent.jmx.values.SolrJmxValues;
import com.newrelic.agent.jmx.values.TomcatJmxValues;
import com.newrelic.agent.jmx.values.WebSphere7JmxValues;
import com.newrelic.agent.jmx.values.WebSphereJmxValues;
import com.newrelic.agent.jmx.values.WeblogicJmxValues;
import com.newrelic.agent.jmx.values.WebsphereLibertyJmxValues;
import com.newrelic.agent.service.ServiceFactory;

import javax.management.MBeanServerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class JmxApiImpl implements JmxApi {

    private final ConcurrentMap<String, Boolean> addedJmx = new ConcurrentHashMap<>();

    @Override
    public void addJmxMBeanGroup(String name) {
        if (!addedJmx.containsKey(name)) {
            JmxFrameworkValues jmx = getJmxFrameworkValues(name);
            if (null != jmx) {
                Boolean alreadyAdded = addedJmx.putIfAbsent(name, Boolean.TRUE);
                if (null == alreadyAdded || !alreadyAdded) {
                    ServiceFactory.getJmxService().addJmxFrameworkValues(jmx);
                    Agent.LOG.log(Level.FINE, "Added JMX for {0}", jmx.getPrefix());
                } else {
                    Agent.LOG.log(Level.FINE, "Skipped JMX. Already added JMX framework: {0}", name);
                }
            } else {
                Agent.LOG.log(Level.FINE, "Skipped JMX. Unknown JMX framework: {0}", name);
            }
        }
    }

    private JmxFrameworkValues getJmxFrameworkValues(String prefixName) {
        JmxService jmxService = ServiceFactory.getJmxService();
        if (prefixName != null) {
            switch (prefixName) {
                case KafkaProducerJmxValues.PREFIX:
                    return new KafkaProducerJmxValues();
                case KafkaConsumerJmxValues.PREFIX:
                    return new KafkaConsumerJmxValues();
                case WebSphere7JmxValues.PREFIX:
                    return new WebSphere7JmxValues();
                case WebSphereJmxValues.PREFIX:
                    return new WebSphereJmxValues();
                case SolrJmxValues.PREFIX:
                    return new SolrJmxValues();
                case Solr7JmxValues.PREFIX:
                    return jmxService.iteratedObjectNameKeysEnabled()
                            ? new Solr7JmxValues()
                            : new NonIteratedSolr7JmxValues();
                case WebsphereLibertyJmxValues.PREFIX:
                    return new WebsphereLibertyJmxValues();
                case TomcatJmxValues.PREFIX:
                    return new TomcatJmxValues();
                case EmbeddedTomcatJmxValues.PREFIX:
                    return new EmbeddedTomcatJmxValues();
                case EmbeddedTomcatDataSourceJmxValues.PREFIX:
                    return new EmbeddedTomcatDataSourceJmxValues();
                case JettyJmxMetrics.PREFIX:
                    return new JettyJmxMetrics();
                case Jboss7UpJmxValues.PREFIX:
                    return new Jboss7UpJmxValues();
                case ResinJmxValues.PREFIX:
                    return new ResinJmxValues();
                case GlassfishJmxValues.PREFIX:
                    return new GlassfishJmxValues();
                case WeblogicJmxValues.PREFIX:
                    return new WeblogicJmxValues();
                default:
            }
        }
        return null;
    }

    @Override
    public void createMBeanServerIfNeeded() {
        if (System.getProperty("com.sun.management.jmxremote") == null && MBeanServerFactory.findMBeanServer(null).isEmpty()) {
            try {
                MBeanServerFactory.createMBeanServer();
                Agent.LOG.log(Level.INFO, "Created a default MBeanServer");
            } catch (Exception e) {
                Agent.LOG.severe("The JMX Service was unable to create a default MBeanServer");
            }
        }
    }

}
