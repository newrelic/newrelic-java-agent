package com.newrelic.agent.bridge;

public interface IdGenerator {

    String generateSpanId();

    String generateTraceId();
}