/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.yaml;

import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TracerFactoryException;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.api.agent.MethodTracerFactory;

public class YmlExtensionPointCutConverter {

    public static final String CLASS_MATCHER_KEY = "class_matcher";
    public static final String METHOD_MATCHER_KEY = "method_matcher";
    public static final String DISPATCHER_KEY = "dispatcher";
    public static final String METRIC_NAME_FORMAT_KEY = "metric_name_format";
    public static final String SKIP_TRANS_KEY = "skip_transaction_trace";
    public static final String IGNORE_TRANS_KEY = "ignore_transaction";
    public static final String TRACER_FACTOR_KEY = "tracer_factory";

    public static ExtensionClassAndMethodMatcher createExtensionPointCut(Map attrs, String defaultMetricPrefix,
            ClassLoader classLoader, String extName) {
        ClassMatcher classMatcher = getClassMatcher(attrs);
        MethodMatcher methodMatcher = getMethodMatcher(attrs);
        boolean dispatcher = getDispatcher(attrs);

        Config newConfig = new BaseConfig(attrs);
        boolean skipTransTrace = newConfig.getProperty(SKIP_TRANS_KEY, Boolean.FALSE);
        boolean ignoreTrans = newConfig.getProperty(IGNORE_TRANS_KEY, Boolean.FALSE);

        String metricPrefix = defaultMetricPrefix;
        String metricName;
        Object format = attrs.get(METRIC_NAME_FORMAT_KEY);
        if (format instanceof String) {
            metricName = format.toString();
        } else if (null == format) {
            metricName = null;
        } else if (format instanceof MetricNameFormatFactory) {
            // sorry - not supported anymore
            Agent.LOG.log(
                    Level.WARNING,
                    MessageFormat.format(
                            "The object property {0} is no longer supported in the agent. The default naming mechanism will be used.",
                            METRIC_NAME_FORMAT_KEY));
            metricName = null;

        } else {
            throw new RuntimeException(MessageFormat.format("Unsupported {0} value", METRIC_NAME_FORMAT_KEY));
        }

        String tracerFactoryNameString = getTracerFactoryName(attrs, defaultMetricPrefix, dispatcher, format,
                classLoader);

        String nameOfExtension = (extName == null) ? "Unknown" : extName;
        return new ExtensionClassAndMethodMatcher(nameOfExtension, metricName, metricPrefix, classMatcher,
                methodMatcher, dispatcher, skipTransTrace, false, ignoreTrans, tracerFactoryNameString);

    }

    private static ClassMatcher getClassMatcher(Map attrs) {
        ClassMatcher classMatcher = PointCutFactory.getClassMatcher(attrs.get(CLASS_MATCHER_KEY));
        if (classMatcher == null) {
            throw new RuntimeException("No class matcher for " + attrs.toString());
        }
        return classMatcher;
    }

    private static MethodMatcher getMethodMatcher(Map attrs) {
        MethodMatcher methodMatcher = PointCutFactory.getMethodMatcher(attrs.get(METHOD_MATCHER_KEY));
        if (methodMatcher == null) {
            throw new RuntimeException("No method matcher for " + attrs.toString());
        }
        return methodMatcher;
    }

    private static boolean getDispatcher(Map attrs) {
        Object dispatcherProp = attrs.get(DISPATCHER_KEY);
        return dispatcherProp != null && Boolean.parseBoolean(dispatcherProp.toString());
    }

    private static String getTracerFactoryName(Map attrs, String prefix, boolean dispatcher, Object metricNameFormat,
            ClassLoader loader) {
        String tracerFactoryNameString = null;

        Object tracerFactoryName = attrs.get(TRACER_FACTOR_KEY);
        if (tracerFactoryName != null) {
            try {
                TracerFactory factory = getTracerFactory(tracerFactoryName.toString(), loader,
                        new TracerFactoryConfiguration(prefix, dispatcher, metricNameFormat, attrs));
                tracerFactoryNameString = tracerFactoryName.toString();
                ServiceFactory.getTracerService().registerTracerFactory(tracerFactoryNameString, factory);
            } catch (TracerFactoryException ex) {
                throw new RuntimeException("Unable to create tracer factory " + tracerFactoryName, ex);
            }
        }
        return tracerFactoryNameString;
    }

    /**
     * Instantiates a tracer factory class of the given name.
     * 
     * @param tracerFactoryName
     * @param config
     * @throws TracerFactoryException
     */
    public static TracerFactory getTracerFactory(String tracerFactoryName, ClassLoader classLoader,
            TracerFactoryConfiguration config) throws TracerFactoryException {
        try {

            Class clazz = classLoader.loadClass(tracerFactoryName);

            String msg = MessageFormat.format("Instantiating custom tracer factory {0}", tracerFactoryName);
            Agent.LOG.finest(msg);
            if (TracerFactory.class.isAssignableFrom(clazz)) {
                return instantiateTracerFactory(clazz, config);
            } else if (MethodTracerFactory.class.isAssignableFrom(clazz)) {
                return instantiateMethodTracerFactory(clazz);
            } else {
                throw new TracerFactoryException("Unknown tracer factory type:" + tracerFactoryName);
            }
        } catch (Exception ex) {
            throw new TracerFactoryException("Unable to load tracer factory " + tracerFactoryName, ex);
        }
    }

    /**
     * Instantiate a MethodTracerFactory instance.
     * 
     * @param clazz
     * @throws Exception
     */
    private static TracerFactory instantiateMethodTracerFactory(Class clazz) throws Exception {
        MethodTracerFactory factory = (MethodTracerFactory) clazz.newInstance();
        return new CustomTracerFactory(factory);
    }

    /**
     * Instantiates a tracer factory class by first looking for a constructor that takes a single
     * {@link TracerFactoryConfiguration} object. If that constructor does not exist it looks for a default constructor.
     * 
     * @param clazz
     * @param config
     * @throws TracerFactoryException
     */
    private static TracerFactory instantiateTracerFactory(Class<? extends TracerFactory> clazz,
            TracerFactoryConfiguration config) throws TracerFactoryException {
        try {
            return clazz.getConstructor(TracerFactoryConfiguration.class).newInstance(config);
        } catch (Exception e) {
            // try the default constructor
        }

        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new TracerFactoryException("Unable to instantiate tracer factory " + clazz.getName(), e);
        }

    }

}
