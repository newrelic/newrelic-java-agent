package com.newrelic.agent.config;

import java.util.Map;

public class ObscuringKeyConfig extends BaseConfig {
    public static final String OBSCURING_KEY = "obscuring_key";
    private final String obscuringKey;

    public ObscuringKeyConfig(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot);
        obscuringKey = getProperty(OBSCURING_KEY);
    }

    public String getObscuringKey() {
        return obscuringKey;
    }
}