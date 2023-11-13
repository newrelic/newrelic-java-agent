package com.newrelic.opentelemetry;

import com.newrelic.api.agent.Logger;

import java.util.logging.Level;

final class NoOpLogger implements Logger {

    private static final NoOpLogger INSTANCE = new NoOpLogger();

    private NoOpLogger() {
    }

    static NoOpLogger getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isLoggable(Level level) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "isLoggable");
        return false;
    }

    @Override
    public void log(Level level, String pattern, Object[] msg) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern, Object part1) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7,
            Object... otherParts) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object[] msg) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6,
            Object part7) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void log(Level level, Throwable t, String pattern, Object part1, Object part2, Object part3, Object part4, Object part5, Object part6, Object part7,
            Object... otherParts) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "log");
    }

    @Override
    public void logToChild(String childName, Level level, String pattern, Object part1, Object part2, Object part3, Object part4) {
        OpenTelemetryNewRelic.logUnsupportedMethod("Logger", "logToChild");
    }
}
