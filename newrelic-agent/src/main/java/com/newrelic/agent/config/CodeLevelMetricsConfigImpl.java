package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

/* Default config should look like:
 *
 * code_level_metrics:
 *   enabled: true
 */
public class CodeLevelMetricsConfigImpl extends BaseConfig implements CodeLevelMetricsConfig {
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + AgentConfigImpl.CODE_LEVEL_METRICS + ".";

    public static final boolean DEFAULT_ENABLED = false;
    public static final String ENABLED = "enabled";

    private final boolean clmEnabled;

    private CodeLevelMetricsConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        clmEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
    }

    @Override
    public boolean isEnabled() {
        return clmEnabled;
    }

    public static CodeLevelMetricsConfig createClmConfig(Map<String, Object> props) {
        if (props == null) {
            props = Collections.emptyMap();
        }
        return new CodeLevelMetricsConfigImpl(props);
    }

}
