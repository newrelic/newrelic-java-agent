/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.config.AgentConfigHelper;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class IntrospectorConfig {

    public static Map<String, Object> readConfig(Class<?> classUnderTest) throws Exception {
        Map<String, Object> config = new HashMap<>();
        for (Annotation annotation : classUnderTest.getAnnotations()) {
            if (annotation instanceof InstrumentationTestConfig) {
                String configPath = ((InstrumentationTestConfig) annotation).configName();
                if (!configPath.isEmpty()) {
                    URL resource = IntrospectorConfig.class.getClassLoader().getResource(configPath);
                    if (resource == null) {
                        throw new InitializationError("Unable to find config file " + configPath);
                    }

                    File configFile = new File(resource.toURI());
                    return AgentConfigHelper.getConfigurationFileSettings(configFile);
                }
            }
        }
        return config;
    }

}
