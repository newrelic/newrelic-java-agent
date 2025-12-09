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

    public static final String DEFAULT_FILE_PATH = "/tmp/newrelic-telemetry";
    public static final Boolean DEFAULT_ENABLED = Boolean.FALSE;

    private final boolean isEnabled;
    private final String filePath;

    public ServerlessConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        filePath = getProperty(FILE_PATH, DEFAULT_FILE_PATH);
    }

    static ServerlessConfigImpl createServerlessConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new ServerlessConfigImpl(settings);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public String filePath() {
        return filePath;
    }

}
