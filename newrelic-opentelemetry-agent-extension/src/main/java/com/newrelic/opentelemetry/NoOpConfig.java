package com.newrelic.opentelemetry;

import com.newrelic.api.agent.Config;

final class NoOpConfig implements Config {

    private static final NoOpConfig INSTANCE = new NoOpConfig();

    private NoOpConfig() {
    }

    static NoOpConfig getInstance() {
        return INSTANCE;
    }

    @Override
    public <T> T getValue(String key) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Config", "getValue");
        return null;
    }

    @Override
    public <T> T getValue(String key, T defaultVal) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Config", "getValue");
        return null;
    }
}
