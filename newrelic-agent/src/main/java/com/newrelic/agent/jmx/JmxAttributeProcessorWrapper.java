/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.util.CleverClassLoader;

import javax.management.Attribute;
import javax.management.ObjectInstance;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class is not thread-safe. Should only be called on the harvest thread.
 */
public class JmxAttributeProcessorWrapper implements JmxAttributeProcessor {

    private static final int MAX_SIZE = 100;
    private final JmxAttributeProcessor JMX_ATTRIBUTE_PROCESSOR_NONE = new JmxAttributeProcessorNone();

    private final String jmxAttributeProcessorClassName;
    private final Map<ClassLoader, JmxAttributeProcessor> jmxAttributeProcessorClasses = new HashMap<>();

    private JmxAttributeProcessorWrapper(String jmxAttributeProcessorClassName) {
        this.jmxAttributeProcessorClassName = jmxAttributeProcessorClassName;
    }

    @Override
    public boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute, String metricName,
            Map<String, Float> values) {
        Object value = attribute.getValue();
        if (value == null || value instanceof Number || value instanceof String || value instanceof Boolean) {
            return false;
        }
        JmxAttributeProcessor processor = getJmxAttributeProcessor(value);
        if (processor == null) {
            return false;
        }
        return processor.process(statsEngine, instance, attribute, metricName, values);
    }

    private JmxAttributeProcessor getJmxAttributeProcessor(Object attributeValue) {
        ClassLoader cl = attributeValue.getClass().getClassLoader();
        cl = cl == null ? AgentBridge.getAgent().getClass().getClassLoader() : cl;
        JmxAttributeProcessor processor = jmxAttributeProcessorClasses.get(cl);
        if (processor == null) {
            try {
                // use reflection to load this class since it refers to classes that aren't always present
                CleverClassLoader classLoader = new CleverClassLoader(cl);
                processor = (JmxAttributeProcessor) classLoader.loadClassSpecial(jmxAttributeProcessorClassName).newInstance();
                if (jmxAttributeProcessorClasses.size() > MAX_SIZE) {
                    jmxAttributeProcessorClasses.clear();
                }
                jmxAttributeProcessorClasses.put(cl, processor);
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format("Loaded {0} using class loader {1}",
                            jmxAttributeProcessorClassName, cl);
                    Agent.LOG.finer(msg);
                }
            } catch (Throwable t) {
                /*
                 * must catch Throwable because loading the JmxAttributeProcessor may throw an error like this:
                 * java.lang.NoClassDefFoundError: javax/management/j2ee/statistics/CountStatistic
                 */
                jmxAttributeProcessorClasses.put(cl, JMX_ATTRIBUTE_PROCESSOR_NONE);
                String msg = MessageFormat.format("Error loading {0} using class loader {1}: {2}",
                        jmxAttributeProcessorClassName, cl, t.toString());
                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    Agent.LOG.log(Level.FINEST, msg, t);
                } else {
                    Agent.LOG.finer(msg);
                }
            }
        }
        return processor;
    }

    protected static JmxAttributeProcessor createInstance(String jmxAttributeProcessorClassName) {
        return new JmxAttributeProcessorWrapper(jmxAttributeProcessorClassName);
    }

    private static class JmxAttributeProcessorNone implements JmxAttributeProcessor {

        @Override
        public boolean process(StatsEngine statsEngine, ObjectInstance instance, Attribute attribute,
                String metricName, Map<String, Float> values) {
            return false;
        }

    }
}
