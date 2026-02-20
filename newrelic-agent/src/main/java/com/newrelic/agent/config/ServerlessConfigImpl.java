/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class ServerlessConfigImpl extends BaseConfig implements ServerlessConfig {

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.serverless_mode.";

    public static final String ENABLED = "enabled";

    public static final String FILE_PATH = "file_path";
    public static final String ARN = "arn";
    public static final String FUNCTION_VERSION = "function_version";

    public static final String DEFAULT_FILE_PATH = "/tmp/newrelic-telemetry";
    public static final Boolean DEFAULT_ENABLED = Boolean.FALSE;

    private final boolean isEnabled;
    private final String filePath;
    private final String arn;
    private final String functionVersion;

    static ServerlessConfigImpl createServerlessConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new ServerlessConfigImpl(settings);
    }

    public ServerlessConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = createIsEnabled();
        filePath = getProperty(FILE_PATH, DEFAULT_FILE_PATH);
        arn = getProperty(ARN);
        functionVersion = getProperty(FUNCTION_VERSION);
    }

    private boolean createIsEnabled() {
        if (AgentConfigHelper.LambdaFunctionNameExists()) {
            return true;
        }
        return getProperty(ENABLED, DEFAULT_ENABLED);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public String filePath() {
        return filePath;
    }

    @Override
    public String getArn() {
        return arn;
    }

    @Override
    public String getFunctionVersion() {
        return functionVersion;
    }

}
