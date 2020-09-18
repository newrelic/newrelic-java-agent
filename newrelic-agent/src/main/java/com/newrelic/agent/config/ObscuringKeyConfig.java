package com.newrelic.agent.config;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.internal.DeepMapClone;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ObscuringKeyConfig extends BaseConfig {
    private static final String OBSCURING_KEY = "obscuring_key";
    private final String obscuringKey;

    public ObscuringKeyConfig(Map<String, Object> props, String parentRoot) {
        super(props, parentRoot);
        obscuringKey = getProperty(OBSCURING_KEY);
    }

    public String getObscuringKey() {
        return obscuringKey;
    }
}