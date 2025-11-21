package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class ServerlessConfigImpl extends BaseConfig implements ServerlessConfig {
    public static final String SERVERLESS_CONFIG_ROOT = "serverless_mode";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.serverless_mode.";

    public static final String ENABLED = "enabled";
    public static final Boolean DEFAULT_ENABLED = Boolean.TRUE;

    private final boolean isEnabled;

    public ServerlessConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
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

}
