/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.config.JmxConfig;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.create.JmxGet;
import com.newrelic.agent.jmx.create.JmxInvoke;
import com.newrelic.agent.jmx.create.JmxObjectFactory;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

public class JmxService extends AbstractService implements HarvestListener {

    private static final int INVOKE_ERROR_COUNT_MAX = 5;
    private static final String J2EE_STATS_ATTRIBUTE_PROCESSOR_CLASS_NAME = "com.newrelic.agent.jmx.J2EEStatsAttributeProcessor";

    private final Set<JmxAttributeProcessor> jmxAttributeProcessors = new HashSet<>();
    /**
     * This is used to create JmxGet and JmxInvoke objects.
     */
    private final JmxObjectFactory jmxMetricFactory;
    /**
     * These are the objects which we want to query the servers for and return metrics.
     */
    private final List<JmxGet> jmxGets = new LinkedList<>();
    /**
     * These are the objects which we need to invoke once on the server, generally to get metrics.
     */
    private final List<JmxInvoke> jmxInvokes = new LinkedList<>();
    /**
     * This is where point cuts add jmx frameworks. Be careful as this is used by multiple threads.
     */
    private final Queue<JmxFrameworkValues> toBeAdded = new ConcurrentLinkedQueue<>();
    /**
     * This is only set if there are special mbean servers we should query.
     */
    private final Set<MBeanServer> alwaysIncludeMBeanServers = new CopyOnWriteArraySet<>();
    /**
     * Remove MBeanServers who class name is contained here.
     */
    private final Set<MBeanServer> toRemoveMBeanServers = new CopyOnWriteArraySet<>();
    private final JmxConfig jmxConfig;

    public JmxService(JmxConfig jmxConfig) {
        super(JmxService.class.getSimpleName());
        this.jmxConfig = jmxConfig;
        jmxMetricFactory = JmxObjectFactory.createJmxFactory();
    }

    @SuppressWarnings("unused") // used in functional_test
    @VisibleForTesting
    public List<JmxGet> getConfigurations() {
        return Collections.unmodifiableList(jmxGets);
    }

    public void addJmxAttributeProcessor(JmxAttributeProcessor attributeProcessor) {
        this.jmxAttributeProcessors.add(attributeProcessor);
    }

    @Override
    protected void doStart() {
        if (jmxConfig.isEnabled()) {
            registerAgentMBeans();
            jmxMetricFactory.getStartUpJmxObjects(jmxGets, jmxInvokes);
            if (jmxGets.size() > 0) {
                ServiceFactory.getHarvestService().addHarvestListener(this);
            }

            addJmxAttributeProcessor(
                    JmxAttributeProcessorWrapper.createInstance(J2EE_STATS_ATTRIBUTE_PROCESSOR_CLASS_NAME));
        }
    }

    @Override
    public final boolean isEnabled() {
        return jmxConfig.isEnabled();
    }

    /**
     * This method can be called by multiple threads. Be careful!!
     */
    public void addJmxFrameworkValues(final JmxFrameworkValues jmxValues) {
        if (jmxConfig.isEnabled()) {
            toBeAdded.add(jmxValues);
        }
    }

    @Override
    protected void doStop() {
        jmxGets.clear();
        jmxInvokes.clear();
        jmxAttributeProcessors.clear();
    }

    @Override
    public void beforeHarvest(String appName, StatsEngine statsEngine) {
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.log(Level.FINER, MessageFormat.format("Harvesting JMX metrics for {0}", appName));
        }
        try {
            process(statsEngine);
        } catch (Exception e) {
            String msg = MessageFormat.format("Unexpected error querying MBeans in JMX service: ", e.toString());
            getLogger().finer(msg);
        }
    }

    @Override
    public void afterHarvest(String appName) {
        // ignore
    }

    /**
     * This should only be called once per a server. Currently this is only used for WebSphere.
     */
    public void setJmxServer(MBeanServer server) {
        if (server != null && !alwaysIncludeMBeanServers.contains(server)) {
            Agent.LOG.log(Level.FINE, "JMX Service : MBeanServer of type {0} was added.", server.getClass().getName());
            alwaysIncludeMBeanServers.add(server);
        }
    }

    /**
     * This should only be called once per a server.
     *
     * @param serverToRemove The server to remove.
     */
    public void removeJmxServer(MBeanServer serverToRemove) {
        if (serverToRemove != null) {
            Agent.LOG.log(Level.FINE, "JMX Service : MBeanServer of type {0} was removed.",
                    serverToRemove.getClass().getName());
            toRemoveMBeanServers.add(serverToRemove);
        }
    }

    public boolean iteratedObjectNameKeysEnabled() {
        return jmxConfig.enableIteratedObjectNameKeys();
    }

    private void process(StatsEngine statsEngine, Collection<MBeanServer> srvrList, JmxGet config) {
        ObjectName name = config.getObjectName();
        if (name == null) {
            return;
        }

        for (MBeanServer server : srvrList) {
            try {
                Set<ObjectInstance> queryMBeans = server.queryMBeans(name, null);
                getLogger().finer(MessageFormat.format("JMX Service : MBeans query {0}, matches {1}", name, queryMBeans.size()));
                Map<ObjectName, Map<String, Float>> mbeanToAttValues = new HashMap<>();
                for (ObjectInstance instance : queryMBeans) {
                    ObjectName actualName = instance.getObjectName();
                    String rootMetricName = config.getRootMetricName(actualName, server);

                    Collection<String> attributes = config.getAttributes();
                    Map<String, Float> values = new HashMap<>();

                    for (String attr : attributes) {
                        getLogger().finest(MessageFormat.format("Fetching attributes for mbean {0}", actualName));
                        getAttAndRecord(attr, name, server, instance, statsEngine, rootMetricName, values);

                    }
                    if (!values.isEmpty()) {
                        // assuming two beans do not have the same objectName for a server
                        mbeanToAttValues.put(actualName, values);
                    }
                }
                config.recordStats(statsEngine, mbeanToAttValues, server);
            } catch (Exception e) {
                getLogger().fine(MessageFormat.format("An error occurred fetching JMX object matching name {0}", name));
                getLogger().log(Level.FINEST, "JMX error", e);
            }
        }
    }

    private void getAttAndRecord(String attr, ObjectName name, MBeanServer server, ObjectInstance instance, StatsEngine statsEngine, String rootMetricName,
            Map<String, Float> values) {
        String[] compNames = attr.split("\\.");
        Object attrObj = getAttribute(name, server, instance, attr, compNames);
        if (attrObj == null) {
            return;
        }
        if (attrObj instanceof Attribute) {
            recordJmxValue(statsEngine, instance, (Attribute) attrObj, rootMetricName, attr, values);
        } else if (attrObj instanceof CompositeDataSupport) {
            if (compNames.length == 2) {
                recordJmxValue(statsEngine, instance, new Attribute(attr,
                                ((CompositeDataSupport) attrObj).get(compNames[1])), rootMetricName, attr, values);
            } else {
                getLogger().fine(
                        MessageFormat.format(
                                "Found CompositeDataSupport object for {0}, but no object attribute specified, correct syntax is object.attribute",
                                attr));
            }
        } else {
            recordJmxValue(statsEngine, instance, new Attribute(attr, attrObj), rootMetricName,
                    attr, values);
        }
    }

    private Object getAttribute(ObjectName name, MBeanServer server, ObjectInstance instance, String attr, String[] compNames) {
        try {
            return server.getAttribute(instance.getObjectName(), attr);
        } catch (AttributeNotFoundException e) {
            getLogger().fine(MessageFormat.format("Attribute {0} for metric {1} was not found", attr, name));
        }
        catch (Exception e) {
            getLogger().fine(
                    MessageFormat.format("An error occurred fetching JMX attribute {0} for metric {1}",
                            attr, name));
            getLogger().log(Level.FINEST, "JMX error", e);
        }

        try {
            return server.getAttribute(instance.getObjectName(), compNames[0]);
        } catch (AttributeNotFoundException e) {
            getLogger().fine(MessageFormat.format("Composite attribute {0} for metric {1} was not found", attr, name));
        }
        catch (Exception e) {
            getLogger().fine(
                    MessageFormat.format(
                            "An error occurred fetching JMX composite attribute {0} for metric {1}",
                            compNames[0], name));
            getLogger().log(Level.FINEST, "JMX error", e);
        }

        return null;
    }

    /**
     * This goes through the invokes. If the invoke is successful on a server, then remove it. Otherwise we attempt the
     * invoke on 5 harvests. If it still fails then we stop trying and remove it.
     *
     * @param srvrList The list of servers.
     */
    private void runThroughAndRemoveInvokes(Collection<MBeanServer> srvrList) {
        if (jmxInvokes.size() > 0) {
            Iterator<JmxInvoke> invokes = jmxInvokes.iterator();
            JmxInvoke current;
            while (invokes.hasNext()) {
                current = invokes.next();
                if (handleInvoke(srvrList, current)) {
                    invokes.remove();
                } else {
                    current.incrementErrorCount();
                    // stop trying after 5 times
                    if (current.getErrorCount() >= INVOKE_ERROR_COUNT_MAX) {
                        invokes.remove();
                    }
                }
            }
        }
    }

    private boolean handleInvoke(Collection<MBeanServer> srvrList, JmxInvoke invoke) {
        ObjectName name = invoke.getObjectName();
        if (name == null) {
            return true;
        }
        // attempt to invoke
        boolean isSuccess = false;
        for (MBeanServer server : srvrList) {
            if (invoke(server, invoke)) {
                isSuccess = true;
            }
        }

        return isSuccess;
    }

    private boolean invoke(MBeanServer server, JmxInvoke current) {
        try {
            server.invoke(current.getObjectName(), current.getOperationName(), current.getParams(),
                    current.getSignature());
            getLogger().fine(
                    MessageFormat.format("Successfully invoked JMX server for {0}", current.getObjectNameString()));
            return true;
        } catch (Exception e) {
            getLogger().fine(
                    MessageFormat.format("An error occurred invoking JMX server for {0}",
                            current.getObjectNameString()));
            getLogger().log(Level.FINEST, "JMX error", e);
            return false;
        }
    }

    private void recordJmxValue(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String rootMetric, String attName, Map<String, Float> values) {
        // you can not sum multiple beans currently for custom JMX metrics (J2EE and Websphere)
        if (recordCustomJmxValue(statsEngine, instance, attribute, rootMetric, values)) {
            return;
        }
        recordNonCustomJmxValue(instance, attribute, attName, values);

    }

    private boolean recordCustomJmxValue(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName, Map<String, Float> values) {
        for (JmxAttributeProcessor processor : jmxAttributeProcessors) {
            if (processor.process(statsEngine, instance, attribute, metricName, values)) {
                return true;
            }
        }
        return false;
    }

    private void recordNonCustomJmxValue(ObjectInstance instance, Attribute attribute, String attName, Map<String, Float> values) {
        Object value = attribute.getValue();
        Number num = null;
        if (value instanceof Number) {
            num = (Number) value;
        } else if (value instanceof Boolean) {
            num = (Boolean) value ? 1 : 0;
        } else if (value != null) {
            try {
                num = Float.parseFloat(value.toString());
            } catch (NumberFormatException e) {
            } // handled below
        }
        if (num != null) {
            getLogger().finer(MessageFormat.format("Recording JMX metric {0} : {1}", attName, value));
            values.put(attName, num.floatValue());
        } else {
            if (value == null) {
                getLogger().fine(
                        MessageFormat.format("MBean {0} attribute {1} value is null", instance.getObjectName(),
                                attName));
            } else {
                getLogger().fine(
                        MessageFormat.format("MBean {0} attribute {1} is not a number ({2}/{3})",
                                instance.getObjectName(), attName, value, value.getClass().getName()));
            }
        }
    }

    private void process(StatsEngine statsEngine) {
        Collection<MBeanServer> srvrList = getServers();
        addNewFrameworks();
        runThroughAndRemoveInvokes(srvrList);

        for (JmxGet object : jmxGets) {
            process(statsEngine, srvrList, object);
        }
    }

    private Collection<MBeanServer> getServers() {
        Collection<MBeanServer> srvrList;
        if (alwaysIncludeMBeanServers.isEmpty() && toRemoveMBeanServers.isEmpty()) {
            // most app servers go here
            srvrList = MBeanServerFactory.findMBeanServer(null);
        } else {

            srvrList = new HashSet<>(MBeanServerFactory.findMBeanServer(null));

            // remove the desired servers
            getLogger().log(Level.FINEST, "JMX Service : toRemove MBeansServers ({0})", toRemoveMBeanServers.size());
            srvrList.removeAll(toRemoveMBeanServers);

            // now include the adds - adds take priority over removes
            getLogger().log(Level.FINEST, "JMX Service : toAdd MBeansServers ({0})", alwaysIncludeMBeanServers.size());
            // we are a set and so these should not be included more than once
            srvrList.addAll(alwaysIncludeMBeanServers);
        }

        getLogger().log(Level.FINER, "JMX Service : querying MBeansServers ({0})", srvrList.size());
        return srvrList;
    }

    /**
     * Grabs any new jmx frameworks which have been provided to the JmxService through point cuts and adds them to the
     * list of jmxObjects.
     */
    private void addNewFrameworks() {
        // be careful with the toBeAdded queue
        JmxFrameworkValues framework = toBeAdded.poll();
        while (framework != null) {
            jmxMetricFactory.convertFramework(framework, jmxGets, jmxInvokes);
            framework = toBeAdded.poll();
        }
    }

    public void reloadExtensions(Set<Extension> oldExtensions, Set<Extension> extensions) {
        for (Iterator<JmxGet> iterator = jmxGets.iterator(); iterator.hasNext(); ) {
            if (oldExtensions.contains(iterator.next().getOrigin())) {
                iterator.remove();
            }
        }
        for (Extension newExtension : extensions) {
            jmxMetricFactory.addExtension(newExtension, jmxGets);
        }
    }

    private void registerAgentMBeans() {
        if (jmxConfig.registerLinkingMetadataMBean()) {
            // This registers the mbean that exposes linking metadata
            new LinkingMetadataRegistration(Agent.LOG).registerLinkingMetadata();
        }
    }

    /*
     * These are examples of the built in metric names.
     *
     * JmxMetric metric = JmxMetric.create("ThreadCount", JmxType.SIMPLE); metric.recordStats(stats,
     * "JmxBuiltIn/Threads/Thread Count", 2f); metric.recordStats(stats, "JmxBuiltIn/ThreadPool/myPool/Active", 3f);
     * metric.recordStats(stats, "JmxBuiltIn/ThreadPool/myPool/Idle", 4f); metric.recordStats(stats,
     * "JmxBuiltIn/ThreadPool/myPool/Max", 5f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/ThreadPool/otherPool/Active", 8f); metric.recordStats(stats,
     * "JmxBuiltIn/ThreadPool/otherPool/Idle", 1f); metric.recordStats(stats, "JmxBuiltIn/ThreadPool/otherPool/Max",
     * 10f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/ThreadPool/yay/Active", 8f); metric.recordStats(stats,
     * "JmxBuiltIn/ThreadPool/yay/Idle", 1f); metric.recordStats(stats, "JmxBuiltIn/ThreadPool/yay/Max", 10f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/ThreadPool/rara/Active", 8f); metric.recordStats(stats,
     * "JmxBuiltIn/ThreadPool/rara/Idle", 1f); metric.recordStats(stats, "JmxBuiltIn/ThreadPool/rara/Max", 10f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/Session/myapp/Active", 5f); metric.recordStats(stats,
     * "JmxBuiltIn/Session/myapp/Rejected", 1f); metric.recordStats(stats, "JmxBuiltIn/Session/myapp/Expired", 30f);
     * metric.recordStats(stats, "JmxBuiltIn/Session/myapp/AverageAliveTime", 30f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/Session/mysecondapp/Active", 5f); metric.recordStats(stats,
     * "JmxBuiltIn/Session/mysecondapp/Rejected", 1f); metric.recordStats(stats,
     * "JmxBuiltIn/Session/mysecondapp/Expired", 30f); metric.recordStats(stats,
     * "JmxBuiltIn/Session/mysecondapp/AverageAliveTime", 30f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/Classes/TotalLoaded", 345f); metric.recordStats(stats,
     * "JmxBuiltIn/Classes/Loaded", 1f); metric.recordStats(stats, "JmxBuiltIn/Classes/Unloaded", 0f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/Transactions/Currently/Active", 5f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/Transactions/Outcome/Committed", 5f); metric.recordStats(stats,
     * "JmxBuiltIn/Transactions/Outcome/Rolled Back", 2f);
     *
     * metric.recordStats(stats, "JmxBuiltIn/Transactions/Created/Top Level", 3f); metric.recordStats(stats,
     * "JmxBuiltIn/Transactions/Created/Nested", 2f);
     */

}
