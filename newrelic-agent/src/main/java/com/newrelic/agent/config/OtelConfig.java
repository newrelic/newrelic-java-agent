package com.newrelic.agent.config;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;

import java.util.List;
import java.util.Map;

public class OtelConfig extends BaseConfig{

    public static final String EXCLUDE_METERS = "meters.exclude";

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.opentelemetry.";

    private final List<String> excludedMeters;

    public OtelConfig(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        excludedMeters = getUniqueStrings(EXCLUDE_METERS);
    }

    public List<String> getExcludedMeters(){
        return excludedMeters;
    }
}
