package com.newrelic.opentelemetry;

import com.newrelic.api.agent.Token;

final class NoOpToken implements Token {

    private static final NoOpToken INSTANCE = new NoOpToken();

    private NoOpToken() {
    }

    static NoOpToken getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean link() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Token", "link");
        return false;
    }

    @Override
    public boolean expire() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Token", "expire");
        return false;
    }

    @Override
    public boolean linkAndExpire() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Token", "linkAndExpire");
        return false;
    }

    @Override
    public boolean isActive() {
        OpenTelemetryNewRelic.logUnsupportedMethod("Token", "isActive");
        return false;
    }
}
