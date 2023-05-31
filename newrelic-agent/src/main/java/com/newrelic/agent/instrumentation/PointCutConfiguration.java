/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.config.Config;
import com.newrelic.agent.service.ServiceFactory;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

/**
 * Stores the configuration information for a pointcut, such as whether or not the pointcut should be enabled.
 */
public class PointCutConfiguration {
    private final String name;
    private final String groupName;
    private final Config pointCutConfig;
    private final Config pointCutGroupConfig;
    private final boolean enabledByDefault;

    public PointCutConfiguration(Class<? extends PointCut> pc) {
        this(pc.getName(), null, true);
    }

    public PointCutConfiguration(String configurationName) {
        this(configurationName, null, true);
    }

    public PointCutConfiguration(String configurationName, boolean enabledByDefault) {
        this(configurationName, null, enabledByDefault);
    }

    public PointCutConfiguration(String configurationName, String configurationGroupName, boolean enabledByDefault) {
        this(configurationName, configurationGroupName, enabledByDefault,
                ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig());
    }

    public PointCutConfiguration(String configurationName, String configurationGroupName,
                                 boolean enabledByDefault, ClassTransformerConfig config) {
        this.name = configurationName;
        this.groupName = configurationGroupName;
        this.enabledByDefault = enabledByDefault;
        this.pointCutConfig = initConfig(configurationName, config);
        this.pointCutGroupConfig = initConfig(configurationGroupName, config);
    }

    public final String getName() {
        return name;
    }

    public final String getGroupName() {
        return this.groupName;
    }

    public Config getConfiguration() {
        return pointCutConfig;
    }

    private Config initConfig(String name, ClassTransformerConfig classTransformerConfig) {
        Map<String, Object> config = Collections.emptyMap();
        if (name != null) {
            Object pointCutConfig = classTransformerConfig.getProperty(
                    name);
            if (pointCutConfig instanceof Map) {
                config = (Map) pointCutConfig;
            }
        }
        return new BaseConfig(config);
    }


    public boolean isEnabled() {
        boolean groupExplicitlyEnabled = pointCutGroupConfig.getProperty("enabled", false);
        boolean classExplicitlyEnabled = pointCutConfig.getProperty("enabled", false);
        if (groupExplicitlyEnabled || classExplicitlyEnabled) {
            Agent.LOG.info(MessageFormat.format("Enabled point cut \"{1}\" (\"{2}\")",  getName(), getGroupName()));
            return true;
        }

        boolean groupExplicitlyDisabled = !pointCutGroupConfig.getProperty("enabled", true);
        boolean classExplicitlyDisabled = !pointCutConfig.getProperty("enabled", true);
        if (groupExplicitlyDisabled || classExplicitlyDisabled) {
            Agent.LOG.info(MessageFormat.format("Disabled point cut \"{1}\" (\"{2}\")",  getName(), getGroupName()));
            return false;
        }

        ClassTransformerConfig classTransformerConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getClassTransformerConfig();
        boolean instrumentationDefaultDisabled = !classTransformerConfig.isDefaultInstrumentationEnabled();
        if (instrumentationDefaultDisabled) {
            String msg = MessageFormat.format("Instrumentation is disabled by default. Disabled point cut \"{0}\" (\"{1}\")", getName(), getGroupName());
            Agent.LOG.info(msg);
            return false;
        }

        return enabledByDefault;
    }

    protected boolean isEnabledByDefault() {
        return enabledByDefault;
    }

}
