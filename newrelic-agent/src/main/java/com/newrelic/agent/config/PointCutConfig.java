/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.yaml.InstrumentationConstructor;
import com.newrelic.agent.instrumentation.yaml.PointCutFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PointCutConfig {
    private static String defaultMetricPrefix;
    private Yaml yaml;
    private final List<ExtensionClassAndMethodMatcher> pcList = new ArrayList<>();

    public static Collection<ExtensionClassAndMethodMatcher> getExtensionPointCuts(Extension extension, Map instrumentation) {
        Collection<ExtensionClassAndMethodMatcher> list = new ArrayList<>();
        if (instrumentation != null) {
            list.addAll(addInstrumentation(extension, instrumentation));
        }
        if (Agent.LOG.isLoggable(Level.FINEST)) {
            for (ExtensionClassAndMethodMatcher pc : list.toArray(new ExtensionClassAndMethodMatcher[0])) {
                String msg = MessageFormat.format("Extension instrumentation point: {0} {1}", pc.getClassMatcher(), pc.getMethodMatcher());
                Agent.LOG.finest(msg);
            }
        }
        return list;
    }

    private static Collection<ExtensionClassAndMethodMatcher> addInstrumentation(Extension ext, Map instrumentation) {
        try {
            Map instrumentationMap = instrumentation;
            defaultMetricPrefix = (String) instrumentationMap.get("metric_prefix");
            defaultMetricPrefix = defaultMetricPrefix == null ? MetricNames.CUSTOM : defaultMetricPrefix;
            Object pcConfig = instrumentationMap.get("pointcuts");
            PointCutFactory pcFactory = new PointCutFactory(AgentBridge.getAgent().getClass().getClassLoader(), defaultMetricPrefix, ext.getName());
            return pcFactory.getPointCuts(pcConfig);
        } catch (Throwable t) {
            String msg = MessageFormat.format("An error occurred reading the pointcuts in extension {0} : {1}", ext.getName(), t.toString());
            Agent.LOG.severe(msg);
            Agent.LOG.log(Level.FINER, msg, t);
        }
        return Collections.emptyList();
    }

    public PointCutConfig(File[] files) {
        if (null != files) {
            initYaml();
            for (File file : files) {
                try {
                    FileInputStream input = new FileInputStream(file);
                    loadYaml(input);
                    Agent.LOG.info(MessageFormat.format("Loaded custom instrumentation from {0}", file.getName()));
                } catch (FileNotFoundException e) {
                    Agent.LOG.warning(MessageFormat.format(
                            "Could not open instrumentation file {0}. Please check that the file exists and has the correct permissions. ",
                            file.getPath()));
                } catch (Exception e) {
                    Agent.LOG.log(Level.SEVERE, MessageFormat.format(
                            "Error loading YAML instrumentation from {0}. Please check the file's format.",
                            file.getName()));
                    Agent.LOG.log(Level.FINER, "YAML error: ", e);
                }
            }
        }
    }

    public PointCutConfig(InputStream input) {
        initYaml();
        try {
            loadYaml(input);
        } catch (Exception e) {
            Agent.LOG.log(Level.SEVERE, "Error loading YAML instrumentation");
            Agent.LOG.log(Level.FINER, "Error: ", e);
        }
    }

    private void initYaml() {
        SafeConstructor constructor = new InstrumentationConstructor(new LoaderOptions());
        yaml = new Yaml(constructor);
    }

    @SuppressWarnings("unchecked")
    private void loadYaml(InputStream input) throws ParseException {
        if (null == input) {
            return;
        }
        Object config = yaml.load(input);
        PointCutFactory pcFactory = new PointCutFactory(AgentBridge.getAgent().getClass().getClassLoader(), MetricNames.CUSTOM, "CustomYaml");
        pcList.addAll(pcFactory.getPointCuts(config));
    }

    public List<ExtensionClassAndMethodMatcher> getPointCuts() {
        return pcList;
    }
}