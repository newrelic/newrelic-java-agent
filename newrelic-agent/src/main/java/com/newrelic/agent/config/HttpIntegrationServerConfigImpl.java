package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class HttpIntegrationServerConfigImpl extends BaseConfig implements HttpIntegrationServerConfig {
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.http_integration_server.";
    public static final String ENABLED = "enabled";
    public static final String PORT = "port";
    public static final Boolean ENABLED_DEFAULT = Boolean.FALSE;
    public static final int PORT_DEFAULT = 7645;

    private boolean enabled;

    public HttpIntegrationServerConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        this.enabled = getProperty(ENABLED, ENABLED_DEFAULT);
    }

    static HttpIntegrationServerConfigImpl createHttpIntegrationServerConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new HttpIntegrationServerConfigImpl(settings);
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int getPort() {
        return getProperty(PORT, PORT_DEFAULT);
    }
}
