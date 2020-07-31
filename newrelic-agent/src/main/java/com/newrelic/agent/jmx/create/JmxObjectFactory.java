/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.create;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.JmxConfig;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxInvokeValue;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JMXMetricType;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Annotations;

public class JmxObjectFactory {

    /** Frameworks which we should not collect metrics for. These should match the prefix in each JmxFramework. */
    private final Collection<String> disabledJmxFrameworks;

    /**
     * 
     * Creates this JmxObjectFactory. This is private because the factory method should be used to create this.
     */
    private JmxObjectFactory() {
        super();
        JmxConfig jmxConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getJmxConfig();
        disabledJmxFrameworks = jmxConfig.getDisabledJmxFrameworks();
    }

    /**
     * Called by the JMX Service to create the factory used to create object names and jmx metrics.
     */
    public static JmxObjectFactory createJmxFactory() {
        return new JmxObjectFactory();
    }

    /**
     * This should be called on startup by the JMX Service. This will load the default jmx values and the yml
     * configuration files.
     */
    public void getStartUpJmxObjects(List<JmxGet> jmxGets, List<JmxInvoke> jmxInvokes) {
        getStoredJmxObjects(jmxGets, jmxInvokes);
        getYmlJmxGets(jmxGets);

    }

    /**
     * Converts the framework jmx values into jmx objects.
     */
    public void convertFramework(JmxFrameworkValues framework, List<JmxGet> jmxGets, List<JmxInvoke> jmxInvokes) {
        if (framework != null) {
            if (isDisabled(framework)) {
                Agent.LOG.log(Level.INFO, MessageFormat.format(
                        "JMX Metrics for the {0} framework are disabled and therefore are not being loaded.",
                        framework.getPrefix()));
            } else {
                convertToJmxGets(framework, jmxGets);
                convertToJmxInvoke(framework, jmxInvokes);
            }
        }
    }

    /**
     * Gets the object safe name.
     * 
     * @param pObjectNameString The input name for the object.
     * @return The safe object name to use.
     */
    protected String getSafeObjectName(final String pObjectNameString) {
        return pObjectNameString;
    }

    private void createLogAddJmxGet(String pObjectName, String rootMetricName,
            Map<JmxType, List<String>> pAttributesToType, List<JmxGet> alreadyAdded, Extension origin) {
        try {
            JmxGet toAdd = new JmxSingleMBeanGet(pObjectName, rootMetricName, getSafeObjectName(pObjectName),
                    pAttributesToType, origin);
            // add at the beginning in case user has same metric then our metric will be taken
            alreadyAdded.add(0, toAdd);
            if (Agent.LOG.isFineEnabled()) {
                Agent.LOG.log(Level.FINER, MessageFormat.format("Adding JMX config: {0}", toAdd));
            }

        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING,
                    "The JMX configuration is invalid and will not be added. Please check your JMX configuration file. The object name is "
                            + pObjectName);
        }
    }

    private void createLogAddJmxGet(JMXMetricType type, String pObjectName, String pRootMetricName,
            List<JmxMetric> pMetrics, JmxAttributeFilter attributeFilter, JmxMetricModifier modifier,
            List<JmxGet> alreadyAdded) {
        try {
            JmxGet toAdd;
            if (type == JMXMetricType.INCREMENT_COUNT_PER_BEAN) {
                toAdd = new JmxSingleMBeanGet(pObjectName, getSafeObjectName(pObjectName), pRootMetricName, pMetrics,
                        attributeFilter, modifier);
            } else {
                toAdd = new JmxMultiMBeanGet(pObjectName, getSafeObjectName(pObjectName), pRootMetricName, pMetrics,
                        attributeFilter, modifier);
            }

            // add at the beginning in case user has same metric then our metric will be taken
            alreadyAdded.add(0, toAdd);
            if (Agent.LOG.isFineEnabled()) {
                Agent.LOG.log(Level.FINER, MessageFormat.format("Adding JMX config: {0}", toAdd));
            }

        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING,
                    "The JMX configuration is invalid and will not be added. Please check your JMX configuration file. The object name is "
                            + pObjectName);
        }
    }

    private void createLogAddJmxInvoke(BaseJmxInvokeValue invoke, List<JmxInvoke> alreadyAdded) {
        try {
            JmxInvoke toAdd = new JmxInvoke(invoke.getObjectNameString(),
                    getSafeObjectName(invoke.getObjectNameString()), invoke.getOperationName(), invoke.getParams(),
                    invoke.getSignature());
            alreadyAdded.add(toAdd);
            if (Agent.LOG.isFineEnabled()) {
                Agent.LOG.log(Level.FINER, MessageFormat.format("Adding JMX config: {0}", toAdd));
            }

        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING,
                    "The JMX configuration is invalid and will not be added. Please check your JMX configuration file. The object name is "
                            + invoke.getObjectNameString());
        }
    }

    private void getStoredJmxObjects(List<JmxGet> gets, List<JmxInvoke> invokes) {
        Collection<Class<?>> classes = new Annotations().getJmxInitClasses();

        if (classes != null) {
            for (Class<?> clazz : classes) {
                if (JmxFrameworkValues.class.isAssignableFrom(clazz)) {
                    convertFramework(loadJmxFrameworkValues(clazz.asSubclass(JmxFrameworkValues.class)), gets, invokes);
                }
            }
        }
    }

    private boolean isDisabled(JmxFrameworkValues current) {
        String framework = current.getPrefix();
        return disabledJmxFrameworks.contains(framework);
    }

    private void convertToJmxInvoke(JmxFrameworkValues framework, List<JmxInvoke> alreadyAdded) {

        List<BaseJmxInvokeValue> values = framework.getJmxInvokers();
        if (values != null) {
            for (BaseJmxInvokeValue value : values) {
                createLogAddJmxInvoke(value, alreadyAdded);
            }
        }

    }

    private void convertToJmxGets(JmxFrameworkValues framework, List<JmxGet> alreadyAdded) {

        List<BaseJmxValue> values = framework.getFrameworkMetrics();
        if (values != null) {
            for (BaseJmxValue value : values) {
                createLogAddJmxGet(value.getType(), value.getObjectNameString(), value.getObjectMetricName(),
                        value.getMetrics(), value.getAttributeFilter(), value.getModifier(), alreadyAdded);
            }
        }

    }

    private <T extends JmxFrameworkValues> JmxFrameworkValues loadJmxFrameworkValues(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();

        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to create jmx framework values in class {0} : {1}",
                    clazz.getName(), e.toString());
            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINE, msg, e);
        }
        return null;
    }

    private void getYmlJmxGets(List<JmxGet> alreadyAdded) {
        for (Extension extension : ServiceFactory.getExtensionService().getInternalExtensions().values()) {
            addExtension(extension, alreadyAdded);
        }
        // we no longer need to add the external extensions here because they get added through
        // reloadExtensions
    }

    public void addExtension(Extension extension, List<JmxGet> alreadyAdded) {
        if (extension.isEnabled()) {
            getStoredJmxGets(extension.getJmxConfig(), alreadyAdded, extension.getName(), extension);
        }
    }

    private void getStoredJmxGets(Collection<JmxConfiguration> configs, List<JmxGet> alreadyAdded,
            String extensionName, Extension origin) {
        for (JmxConfiguration parser : configs) {
            boolean isEnabled = parser.getEnabled();
            if (isEnabled) {
                String objectNameString = parser.getObjectName();
                if ((objectNameString == null) || (objectNameString.trim().length() == 0)) {
                    Agent.LOG.log(Level.WARNING,
                            "Not recording JMX metric because the object name is null or empty in extension "
                                    + extensionName);
                } else {
                    Map<JmxType, List<String>> attrs = parser.getAttrs();
                    if ((attrs == null) || (attrs.size() == 0)) {
                        Agent.LOG.log(
                                Level.WARNING,
                                MessageFormat.format(
                                        "Not recording JMX metric with object name {0} in extension {1} because there are no attributes.",
                                        objectNameString, extensionName));
                    } else {
                        // assume all yml configuration files are from the user
                        createLogAddJmxGet(objectNameString, parser.getRootMetricName(), attrs, alreadyAdded, origin);
                    }
                }
            }
        }
    }

}
