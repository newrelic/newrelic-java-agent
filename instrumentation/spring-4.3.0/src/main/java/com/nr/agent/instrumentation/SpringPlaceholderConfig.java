package com.nr.agent.instrumentation;

import com.newrelic.api.agent.NewRelic;

public class SpringPlaceholderConfig {

    public static final boolean springPlaceholderValue = NewRelic.getAgent().getConfig()
            .getValue("spring.placeholder_value.enabled", false);

    private SpringPlaceholderConfig() {
    }

}