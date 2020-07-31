/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic;

import com.newrelic.agent.config.AgentConfigHelper;

import java.io.InputStream;
import java.util.Map;

public class EnvironmentHolderSettingsGenerator {

    private final String ymlFileName;
    private final String environment;
    private final ClassLoader classLoader;

    public EnvironmentHolderSettingsGenerator(String ymlFileName, String environment, ClassLoader classLoader) {
        this.ymlFileName = ymlFileName;
        this.environment = environment;
        this.classLoader = classLoader;
    }

    public Map<String, Object> generateSettings() throws Exception {
        System.setProperty(AgentConfigHelper.NEWRELIC_ENVIRONMENT_SYSTEM_PROP, environment);
        InputStream configFile = classLoader.getResourceAsStream(ymlFileName);
        return AgentConfigHelper.parseConfiguration(configFile);
    }
}
